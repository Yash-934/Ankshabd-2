package com.pocketdev.ank_shabd.ai

import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.Flashcard
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class AiWordCandidateSuggestion(
    val word: String,
    val meaningHi: String,
    val meaningEn: String = "",
    val transliteration: String = "",
    val category: String = "संज्ञा",
    val whyGoodPeg: String = "",
    val mnemonicStory: String = "",
    val visualAssociation: String = "",
    val qualityGrade: String = "B",
    val commonness: String = "medium"
)

data class RejectedCandidateAudit(
    val candidateWord: String,
    val targetCode: String,
    val calculatedCode: String,
    val traceSummary: String,
    val reasonHi: String
)

interface AiProvider {
    val providerName: String
    val isConfigured: Boolean

    suspend fun suggestHindiWordCandidates(targetCode: String, ruleSet: EncodingRuleSet): List<String>
    suspend fun suggestStructuredHindiCandidates(
        targetCode: String,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3,
        rejectedFeedback: List<RejectedCandidateAudit> = emptyList(),
        count: Int = 6
    ): List<AiWordCandidateSuggestion>
    suspend fun translateText(text: String, sourceLang: String, targetLang: String): String
    suspend fun explainWord(word: String, lang: String): String
    suspend fun generateMemoryStory(approvedPegs: List<String>): String
    suspend fun summarizeNote(content: String): String
    suspend fun generateFlashcards(content: String): List<Flashcard>
}

class GeminiAiProvider(
    private var apiKey: String? = null,
    private var model: String = "gemini-2.5-flash",
    private var customEndpoint: String = "",
    private var useCustomProvider: Boolean = false
) : AiProvider {

    override val providerName: String
        get() = if (useCustomProvider) "Custom AI Provider" else "Google Gemini AI"
    override val isConfigured: Boolean
        get() = !apiKey.isNullOrBlank()

    fun updateApiKey(key: String?) {
        apiKey = key?.trim()
    }

    fun configure(provider: String, endpoint: String, selectedModel: String) {
        useCustomProvider = provider == "custom"
        customEndpoint = endpoint.trim()
        model = selectedModel.trim().ifBlank { if (useCustomProvider) "gpt-4o-mini" else "gemini-2.5-flash" }
    }

    fun providerId(): String = if (useCustomProvider) "custom" else "gemini"
    fun endpoint(): String = customEndpoint
    fun modelId(): String = model

    fun getApiKey(): String = apiKey ?: ""

    fun saveApiKey(key: String) {
        apiKey = key.trim()
    }

    suspend fun testConnection(): Pair<Boolean, String> {
        if (!isConfigured) {
            return Pair(false, "API key missing; add one in Settings or AI Studio Secrets.")
        }
        return try {
            val response = requestText("Reply with exactly: AnkShabd connection successful.")
            if (response.isNotBlank()) Pair(true, "Connected to $model.")
            else Pair(false, "Provider returned an empty response.")
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "AI provider connection failed.")
        }
    }

    /**
     * Multi-pass candidate discovery with deterministic V2.3 encoder verification.
     * Gemini proposes candidates; PhoneticMatraEncoder is the sole authority on whether they match [code].
     */
    suspend fun discoverCandidatesForCode(
        code: String,
        count: Int = 5,
        onRejectedAudit: ((List<RejectedCandidateAudit>) -> Unit)? = null
    ): List<PegWord> {
        val sanitizedCode = sanitizeNumberInput(code)
        if (sanitizedCode.isBlank()) return emptyList()

        val verifiedPegs = mutableListOf<PegWord>()
        val rejectedAudits = mutableListOf<RejectedCandidateAudit>()

        if (isConfigured) {
            try {
                // Pass 1: Structured V2.3 candidate generation
                val pass1 = suggestStructuredHindiCandidates(
                    targetCode = sanitizedCode,
                    ruleSet = EncodingRuleSet.V2_3,
                    rejectedFeedback = emptyList(),
                    count = count + 3
                )
                validateAndCollectCandidates(
                    rawSuggestions = pass1,
                    targetCode = sanitizedCode,
                    verifiedOut = verifiedPegs,
                    rejectedOut = rejectedAudits
                )

                // Pass 2: Self-correction retry if Pass 1 yielded no valid matches and we have encoder feedback
                if (verifiedPegs.isEmpty() && rejectedAudits.isNotEmpty()) {
                    val pass2 = suggestStructuredHindiCandidates(
                        targetCode = sanitizedCode,
                        ruleSet = EncodingRuleSet.V2_3,
                        rejectedFeedback = rejectedAudits.take(6),
                        count = count + 3
                    )
                    validateAndCollectCandidates(
                        rawSuggestions = pass2,
                        targetCode = sanitizedCode,
                        verifiedOut = verifiedPegs,
                        rejectedOut = rejectedAudits
                    )
                }
            } catch (_: Exception) {
                // Fall through to deterministic local lexicon lookup if network/API fails
            }
        }

        // Also include any deterministic matches from the local verified lexicon not yet in verifiedPegs
        val localPegs = SeedDictionary.getValidatedSeedPegs(EncodingRuleSet.V2_3)
            .filter { it.code == sanitizedCode }
        for (local in localPegs) {
            if (verifiedPegs.none { it.normalizedWord == local.normalizedWord }) {
                verifiedPegs.add(local)
            }
        }

        onRejectedAudit?.invoke(rejectedAudits.distinctBy { it.candidateWord })
        return verifiedPegs.take(count.coerceAtLeast(5))
    }

    private fun validateAndCollectCandidates(
        rawSuggestions: List<AiWordCandidateSuggestion>,
        targetCode: String,
        verifiedOut: MutableList<PegWord>,
        rejectedOut: MutableList<RejectedCandidateAudit>
    ) {
        for (cand in rawSuggestions) {
            val cleanWord = cand.word.trim()
            if (cleanWord.isBlank()) continue
            val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(cleanWord, EncodingRuleSet.V2_3)
            if (enc.isValid && enc.code == targetCode) {
                if (verifiedOut.any { it.normalizedWord == enc.normalizedWord }) continue
                val grade = when (cand.qualityGrade.uppercase()) {
                    "A" -> "A"
                    "B" -> "B"
                    else -> "Unverified"
                }
                val meaningHi = cand.meaningHi.trim().ifBlank { "$cleanWord (सार्थक हिंदी शब्द)" }
                val visual = cand.visualAssociation.trim().ifBlank { meaningHi }
                val story = cand.mnemonicStory.trim().ifBlank {
                    SeedDictionary.buildVividMnemonicStory(
                        word = enc.normalizedWord,
                        code = enc.code,
                        meaningHi = meaningHi,
                        visualAssociation = visual
                    )
                }
                val whyGood = cand.whyGoodPeg.trim().ifBlank {
                    SeedDictionary.buildWhyGoodPeg(
                        word = enc.normalizedWord,
                        code = enc.code,
                        category = cand.category.ifBlank { "संज्ञा" },
                        meaningHi = meaningHi,
                        qualityGrade = grade
                    )
                }
                verifiedOut.add(
                    PegWord(
                        id = "ai_${enc.normalizedWord}_${enc.code}_${UUID.randomUUID().toString().take(6)}",
                        word = enc.normalizedWord,
                        normalizedWord = enc.normalizedWord,
                        code = enc.code,
                        ruleSetId = EncodingRuleSet.DEFAULT_RULESET_ID,
                        category = cand.category.trim().ifBlank { "खोज (AI Discovered)" },
                        meaningHi = meaningHi,
                        meaningEn = cand.meaningEn.trim().ifBlank { "Hindi peg word for $targetCode" },
                        transliteration = cand.transliteration.trim(),
                        qualityGrade = grade,
                        verificationStatus = "unverified",
                        verificationSource = "gemini_generated",
                        visualAssociation = visual,
                        mnemonicStory = story,
                        encodingBreakdown = enc.formattedTraceSummary(),
                        encoderVersion = PhoneticMatraEncoder.ENCODER_VERSION,
                        dictionaryEvidence = whyGood,
                        commonness = cand.commonness.ifBlank { "medium" },
                        confidence = if (grade == "A") 0.92f else 0.80f
                    )
                )
            } else {
                val reason = if (!enc.isValid) {
                    "अमान्य संरचना: ${enc.warnings.joinToString("; ")}"
                } else {
                    "कोड भिन्नता: अपेक्षित '$targetCode' था, पर V2.3 एनकोडर से '${enc.code}' निकला"
                }
                rejectedOut.add(
                    RejectedCandidateAudit(
                        candidateWord = cleanWord,
                        targetCode = targetCode,
                        calculatedCode = enc.code,
                        traceSummary = enc.formattedTraceSummary(),
                        reasonHi = reason
                    )
                )
            }
        }
    }

    override suspend fun suggestStructuredHindiCandidates(
        targetCode: String,
        ruleSet: EncodingRuleSet,
        rejectedFeedback: List<RejectedCandidateAudit>,
        count: Int
    ): List<AiWordCandidateSuggestion> {
        val cleanCode = sanitizeNumberInput(targetCode)
        if (!isConfigured) {
            return generateLocalStructuredCandidates(cleanCode)
        }

        val prompt = buildV23DiscoveryPrompt(
            targetCode = cleanCode,
            rejectedFeedback = rejectedFeedback,
            count = count
        )
        val rawResponse = requestText(prompt, expectJson = true)
        return parseStructuredCandidatesJson(rawResponse, cleanCode)
    }

    override suspend fun suggestHindiWordCandidates(targetCode: String, ruleSet: EncodingRuleSet): List<String> {
        val cleanCode = sanitizeNumberInput(targetCode)
        if (!isConfigured) {
            return generateLocalPhoneticCandidates(cleanCode)
        }
        val discovered = discoverCandidatesForCode(cleanCode, count = 6)
        if (discovered.isNotEmpty()) {
            return discovered.map { it.word }
        }
        throw IOException("AI ने नियम $cleanCode से मेल खाता कोई हिंदी शब्द नहीं दिया।")
    }

    fun buildV23DiscoveryPrompt(
        targetCode: String,
        rejectedFeedback: List<RejectedCandidateAudit> = emptyList(),
        count: Int = 6
    ): String {
        val digitGuide = PhoneticMatraEncoder.explainDigitConstruction(targetCode)
        val feedbackSection = if (rejectedFeedback.isNotEmpty()) {
            val auditLines = rejectedFeedback.joinToString("\n") {
                "- '${it.candidateWord}' अस्वीकृत: इसका वास्तविक V2.3 कोड '${it.calculatedCode}' निकला (${it.traceSummary}) — ${it.reasonHi}"
            }
            """
            ⚠️ पिछले प्रयास की त्रुटियाँ (इन्हें न दोहराएँ और अतिरिक्त/छूटे हुए अंक सुधारें):
            $auditLines
            """.trimIndent()
        } else {
            ""
        }

        return """
            आप 'हिंदी फोनेटिक एवं मात्रा नंबर मेमोरी सिस्टम V2.3' के विशेषज्ञ हैं।
            लक्ष्य संख्या (Target Number Code): "$targetCode" (ठीक ${targetCode.length} अंक, शुरुआती शून्य सहित क्रम न बदलें)।

            आपका कार्य: ऐसे $count वास्तविक, अर्थपूर्ण, याद रखने योग्य एकल हिंदी शब्द (देवनागरी में) सुझाएँ जिनका V2.3 नियमों से अक्षर-दर-अक्षर कोड ठीक "$targetCode" निकलता हो।

            === सख्त V2.3 मास्टर मैपिंग (केवल यही नियम मान्य हैं) ===
            1. व्यंजन मैपिंग:
               - 0: श, ष, ज़
               - 1: क, ख, ग, घ, क़, ख़, ग़
               - 2: द, ध, ड, ढ, ड़, ढ़
               - 3: त, थ, ट, ठ
               - 4: च, ज, र
               - 5: प, फ, फ़
               - 6: छ, झ
               - 7: स
               - 8: ब, भ
               - 9: न, म, ण
               - वाहक व्यंजन (कोई व्यंजन अंक नहीं): य, व, ह, ल

            2. स्वतंत्र स्वर, मात्राएँ और चिह्न मैपिंग:
               - 0: उ, ऊ | मात्रा: ु, ू
               - 1: ए, ऐ | मात्रा: े, ै
               - 2: ओ, औ | मात्रा: ो, ौ
               - 3: इ, ई | मात्रा: ि, ी
               - 4: ऋ | मात्रा: ृ
               - 5: अनुस्वार (ं) = 5
               - 6: विसर्ग (ः) = 6
               - 8: आ | मात्रा: ा = 8
               - कोई अंक नहीं (∅): अ (स्वतंत्र या व्यंजन में अंतर्निहित अ), चंद्रबिंदु (ँ), हलंत (्)

            3. अति-महत्वपूर्ण नियम (सावधानियाँ):
               - शब्द के सभी लिखित वर्ण बाएँ से दाएँ क्रम में गिने जाते हैं। कोई भी अंतिम व्यंजन या मात्रा छोड़ी नहीं जाती!
               - उदाहरण:
                 * खेल = ख(1) + े(1) + ल(वाहक) = 11
                 * केक = क(1) + े(1) + क(1) = 111
                 * गेहूँ = ग(1) + े(1) + ह(वाहक) + ू(0) + ँ(∅) = 110
                 * गाय = ग(1) + ा(8) + य(वाहक) = 18
                 * दही = द(2) + ह(वाहक) + ी(3) = 23
                 * दवा = द(2) + व(वाहक) + ा(8) = 28
                 * शहर = श(0) + ह(वाहक) + र(4) = 04
                 * हंस = ह(वाहक) + ं(5) + स(7) = 57 (जबकि हँस = 7)
                 * गांव = ग(1) + ा(8) + ं(5) + व(वाहक) = 185
                 * तपः = त(3) + प(5) + ः(6) = 356
                 * कुहुक = क(1) + ु(0) + ह(वाहक) + ु(0) + क(1) = 1001
               - वाहक अक्षर (य, व, ह, ल) का खुद कोई अंक नहीं होता, लेकिन उन पर लगी मात्रा (जैसे ा=8, ी=3, ो=2) का अंक गिना जाता है!
               - यदि शब्द के अंत में अतिरिक्त अंक नहीं चाहिए, तो अंतिम अक्षर बिना मात्रा वाला व्यंजन रखें या वाहक व्यंजन (य, व, ह, ल) रखें। गलती से अंत में 'ा' (8) या 'ी' (3) न लगाएँ जब तक लक्ष्य कोड में 8 या 3 न हो!

            === लक्ष्य कोड "$targetCode" के लिए आवश्यक अक्षर/मात्रा क्रम ===
            $digitGuide

            $feedbackSection

            केवल नीचे दिए गए JSON array प्रारूप में उत्तर दें (कोई अतिरिक्त टेक्स्ट न लिखें):
            [
              {
                "word": "देवनागरी शब्द (बिना स्पेस के एकल शब्द)",
                "meaning_hi": "सरल और स्पष्ट हिंदी में शब्द का अर्थ",
                "meaning_en": "Simple English meaning",
                "transliteration": "Roman Hindi pronunciation",
                "category": "संज्ञा / क्रिया / विशेषण",
                "why_good_peg": "यह शब्द याद रखने के लिए अच्छा मेमोरी पेग क्यों है (हिंदी में 1 वाक्य)",
                "mnemonic_story": "संख्या $targetCode और इस शब्द को जोड़ने वाला एक जीवंत, रंगीन मानसिक चित्र या छोटी हिंदी कहानी",
                "visual_association": "संक्षिप्त दृश्य संकेत (3-6 शब्द)",
                "quality_grade": "A या B (रोज़मर्रा के ठोस शब्द के लिए A, साहित्यिक/यौगिक के लिए B)",
                "commonness": "high या medium"
              }
            ]
        """.trimIndent()
    }

    internal fun parseStructuredCandidatesJson(
        rawResponse: String,
        targetCode: String
    ): List<AiWordCandidateSuggestion> {
        val cleaned = rawResponse.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonArrayStr = if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned
        } else {
            val inner = cleaned.substringAfter("[", "").substringBeforeLast("]", "")
            if (inner.isNotBlank()) "[$inner]" else ""
        }

        if (jsonArrayStr.isBlank()) return emptyList()

        return try {
            val arr = JSONArray(jsonArrayStr)
            val list = mutableListOf<AiWordCandidateSuggestion>()
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                when (item) {
                    is JSONObject -> {
                        val word = item.optString("word", "").trim()
                        if (word.isNotBlank()) {
                            list.add(
                                AiWordCandidateSuggestion(
                                    word = word,
                                    meaningHi = item.optString("meaning_hi", item.optString("meaningHi", "")).trim(),
                                    meaningEn = item.optString("meaning_en", item.optString("meaningEn", "")).trim(),
                                    transliteration = item.optString("transliteration", "").trim(),
                                    category = item.optString("category", "संज्ञा").trim(),
                                    whyGoodPeg = item.optString("why_good_peg", item.optString("whyGoodPeg", "")).trim(),
                                    mnemonicStory = item.optString("mnemonic_story", item.optString("mnemonicStory", "")).trim(),
                                    visualAssociation = item.optString("visual_association", item.optString("visualAssociation", "")).trim(),
                                    qualityGrade = item.optString("quality_grade", item.optString("qualityGrade", "B")).trim(),
                                    commonness = item.optString("commonness", "medium").trim()
                                )
                            )
                        }
                    }
                    is String -> {
                        val word = item.trim()
                        if (word.isNotBlank()) {
                            list.add(
                                AiWordCandidateSuggestion(
                                    word = word,
                                    meaningHi = "$word (सार्थक हिंदी शब्द)",
                                    meaningEn = "Hindi candidate for $targetCode"
                                )
                            )
                        }
                    }
                }
            }
            list.distinctBy { it.word }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun generateText(prompt: String): String {
        if (!isConfigured) return "AI प्रदाता कॉन्फ़िगर नहीं है। सेटिंग्स में API key जोड़ें।"
        return requestText(prompt)
    }

    override suspend fun translateText(text: String, sourceLang: String, targetLang: String): String {
        if (!isConfigured) {
            return "अनुवाद (ऑफ़लाइन स्थानीय कोश)"
        }
        return requestText(
            "Translate the following text from $sourceLang to $targetLang. Return only the translation, preserving meaning and formatting:\n$text"
        )
    }

    override suspend fun explainWord(word: String, lang: String): String {
        val enc = PhoneticMatraEncoder.encode(word)
        if (!isConfigured) {
            return "$word (V2.3 कोड: ${enc.code}, घटक: ${enc.formattedTraceSummary()}): हिंदी भाषा का एक अर्थपूर्ण शब्द।"
        }
        return try {
            requestText(
                "हिंदी शब्द '$word' (V2.3 कोड: ${enc.code}) का सरल हिंदी अर्थ, उदाहरण वाक्य, और इसे याद रखने का एक जीवंत मानसिक चित्र संक्षेप में लिखें।"
            )
        } catch (_: Exception) {
            "$word (V2.3 कोड: ${enc.code}): हिंदी भाषा का एक अर्थपूर्ण शब्द।"
        }
    }

    override suspend fun generateMemoryStory(approvedPegs: List<String>): String {
        if (approvedPegs.isEmpty()) return "कहानी के लिए कोई शब्द उपलब्ध नहीं है।"
        if (!isConfigured) {
            return "एक दिन ${approvedPegs.joinToString(" और ")} आपस में मिले। उन्होंने मिलकर एक अद्भुत रोमांचक दृश्य बनाया जो हमेशा याद रहेगा।"
        }
        return try {
            requestText(
                "इन हिंदी मेमोरी पेग शब्दों को ठीक इसी क्रम में जोड़ते हुए 2-3 वाक्यों की एक अत्यंत जीवंत, मजेदार और तुरंत याद रहने वाली हिंदी दृश्य कहानी (Mnemonic Story) बनाएँ: ${approvedPegs.joinToString(" → ")}"
            )
        } catch (_: Exception) {
            "एक दिन ${approvedPegs.joinToString(" और ")} आपस में मिले। उन्होंने मिलकर एक अद्भुत रोमांचक दृश्य बनाया जो हमेशा याद रहेगा।"
        }
    }

    override suspend fun summarizeNote(content: String): String {
        val lines = content.lines().filter { it.isNotBlank() }
        val bulletPoints = lines.take(4).joinToString("\n") { "• $it" }
        return "### सारांश:\n$bulletPoints"
    }

    override suspend fun generateFlashcards(content: String): List<Flashcard> {
        val cards = mutableListOf<Flashcard>()
        val words = content.split(Regex("\\s+")).filter { it.length > 2 }.distinct().take(3)
        for (w in words) {
            val enc = PhoneticMatraEncoder.encode(w)
            cards.add(
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    cardType = "WORD_TO_NUMBER",
                    prompt = "नोट से निकाला गया शब्द: '$w' का कोड क्या है?",
                    answer = enc.code,
                    explanation = enc.formattedTraceSummary()
                )
            )
        }
        return cards
    }

    private fun generateLocalPhoneticCandidates(targetCode: String): List<String> {
        return SeedDictionary.SEED_ENTRIES
            .filter { PhoneticMatraEncoder.encode(it.word).code == targetCode }
            .map { it.word }
            .distinct()
    }

    private fun generateLocalStructuredCandidates(targetCode: String): List<AiWordCandidateSuggestion> {
        return SeedDictionary.SEED_ENTRIES
            .filter { PhoneticMatraEncoder.encode(it.word).code == targetCode }
            .map { seed ->
                val enc = PhoneticMatraEncoder.encode(seed.word)
                AiWordCandidateSuggestion(
                    word = seed.word,
                    meaningHi = seed.meaningHi,
                    meaningEn = seed.meaningEn,
                    transliteration = seed.transliteration,
                    category = seed.category,
                    whyGoodPeg = SeedDictionary.buildWhyGoodPeg(
                        word = seed.word,
                        code = enc.code,
                        category = seed.category,
                        meaningHi = seed.meaningHi,
                        qualityGrade = seed.qualityGrade,
                        existingReason = seed.whyGoodPeg
                    ),
                    mnemonicStory = SeedDictionary.buildVividMnemonicStory(
                        word = seed.word,
                        code = enc.code,
                        meaningHi = seed.meaningHi,
                        visualAssociation = seed.visualAssociation,
                        existingStory = seed.mnemonicStory
                    ),
                    visualAssociation = seed.visualAssociation ?: seed.meaningHi,
                    qualityGrade = seed.qualityGrade,
                    commonness = if (seed.qualityGrade == "A") "high" else "medium"
                )
            }
    }

    private suspend fun requestText(prompt: String, expectJson: Boolean = false): String = withContext(Dispatchers.IO) {
        val endpoint = if (useCustomProvider) {
            customEndpoint.ifBlank { throw IOException("Custom AI endpoint is not set.") }
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        }
        if (!URL(endpoint).protocol.equals("https", ignoreCase = true)) {
            throw IOException("AI API endpoint must use HTTPS.")
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (useCustomProvider) {
                setRequestProperty("Authorization", "Bearer ${apiKey.orEmpty()}")
            } else {
                setRequestProperty("x-goog-api-key", apiKey.orEmpty())
            }
        }
        try {
            val body = if (useCustomProvider) {
                JSONObject()
                    .put("model", model)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                    .toString()
            } else {
                val payload = JSONObject()
                    .put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        )
                    )
                if (expectJson) {
                    payload.put(
                        "generationConfig",
                        JSONObject()
                            .put("responseMimeType", "application/json")
                            .put("temperature", 0.35)
                    )
                }
                payload.toString()
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("AI provider returned HTTP $status: $responseText")
            val response = JSONObject(responseText)
            val generated = if (useCustomProvider) {
                response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            } else {
                response.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
            }
            generated?.takeIf(String::isNotBlank) ?: throw IOException("AI provider returned no text.")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /**
         * Normalizes ASCII and Devanagari digits (०-९ -> 0-9), preserving leading zeros.
         */
        fun sanitizeNumberInput(raw: String): String {
            val sb = StringBuilder()
            for (ch in raw.trim()) {
                when (ch) {
                    in '0'..'9' -> sb.append(ch)
                    in '०'..'९' -> sb.append(('0'.code + (ch.code - '०'.code)).toChar())
                }
            }
            return sb.toString()
        }
    }
}
