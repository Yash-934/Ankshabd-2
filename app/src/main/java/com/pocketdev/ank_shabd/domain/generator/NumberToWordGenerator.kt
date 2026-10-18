package com.pocketdev.ank_shabd.domain.generator

import com.pocketdev.ank_shabd.ai.GeminiAiProvider
import com.pocketdev.ank_shabd.ai.RejectedCandidateAudit
import com.pocketdev.ank_shabd.data.repository.DictionaryRepository
import com.pocketdev.ank_shabd.domain.encoder.ComponentTrace
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import kotlin.math.abs

/**
 * Represents a single validated Hindi word candidate with all 9 mandatory V2.3 fields.
 */
data class GeneratedWordCandidate(
    val enteredNumber: String,                // 1. Entered number
    val hindiWord: String,                    // 2. Best matching Hindi word
    val meaningHi: String,                    // 3. Meaning of the word in simple Hindi
    val meaningEn: String,
    val characterByCharacterEncoding: String, // 4. Exact character-by-character encoding
    val traces: List<ComponentTrace>,
    val finalEncodedNumber: String,           // 5. Final encoded number (from PhoneticMatraEncoder)
    val whyGoodMnemonicPeg: String,           // 6. Why the word is a good mnemonic peg
    val vividMentalStory: String,             // 7. A vivid, memorable mental image or short Hindi story
    val qualityGrade: String,                 // 8. Word quality: A, B, or Unverified
    val verificationStatus: String,           // 9. Verification status (approved / unverified)
    val verificationSource: String,
    val pegWord: PegWord,
    val rankScore: Int
)

/**
 * Records each step of the progressive 1-word -> 2-words -> 3-words ... attempt loop.
 */
data class WordCountAttemptStep(
    val wordCount: Int,
    val succeeded: Boolean,
    val matchedSplits: List<String>,
    val matchedWords: List<String>,
    val summaryHi: String
)

data class SegmentPegItem(
    val segmentCode: String,
    val word: String,
    val meaningHi: String,
    val traceSummary: String,
    val qualityGrade: String,
    val verificationStatus: String,
    val pegWord: PegWord?
)

data class SegmentedNumberBreakdown(
    val enteredNumber: String,
    val wordCountUsed: Int,
    val attemptSteps: List<WordCountAttemptStep>,
    val segments: List<SegmentPegItem>,
    val combinedWordChain: String,
    val combinedEncodingTrace: String,
    val combinedVividStoryHi: String,
    val whySegmentedExplanationHi: String
)

data class NumberGenerationResponse(
    val enteredNumber: String,
    val bestCandidate: GeneratedWordCandidate?,
    val allCandidates: List<GeneratedWordCandidate>,
    val rejectedAiCandidates: List<RejectedCandidateAudit>,
    val segmentedBreakdown: SegmentedNumberBreakdown?,
    val progressiveAttempts: List<WordCountAttemptStep>,
    val resolvedWordCount: Int,
    val digitConstructionGuide: String,
    val usedAiDiscovery: Boolean,
    val statusBannerHi: String
)

class NumberToWordGenerator(
    private val dictionaryRepository: DictionaryRepository?,
    private val aiProvider: GeminiAiProvider?
) {

    /**
     * Progressive V2.3 Number-to-Word pipeline:
     * 1. First tries to build 1 single meaningful Hindi word (k = 1) for the entire number.
     * 2. If 1 single word cannot be formed, tries splitting into 2 meaningful Hindi words (k = 2).
     * 3. If 2 words cannot be formed, tries 3 words (k = 3), and so on until the minimum number
     *    of rule-compliant meaningful Hindi words is found.
     */
    suspend fun generateForNumber(
        rawNumber: String,
        invokeAi: Boolean = true,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3
    ): NumberGenerationResponse {
        val targetCode = GeminiAiProvider.sanitizeNumberInput(rawNumber)
        if (targetCode.isEmpty()) {
            return NumberGenerationResponse(
                enteredNumber = "",
                bestCandidate = null,
                allCandidates = emptyList(),
                rejectedAiCandidates = emptyList(),
                segmentedBreakdown = null,
                progressiveAttempts = emptyList(),
                resolvedWordCount = 0,
                digitConstructionGuide = "",
                usedAiDiscovery = false,
                statusBannerHi = "कृपया कोई संख्या (अंक) दर्ज करें।"
            )
        }

        val rejectedAudits = mutableListOf<RejectedCandidateAudit>()
        val collectedPegs = mutableListOf<PegWord>()
        val progressiveAttempts = mutableListOf<WordCountAttemptStep>()
        var usedAi = false

        // === STEP k = 1: Try to find or generate 1 SINGLE meaningful Hindi word for the entire number ===
        // 1A. Load existing words from SQLite DictionaryRepository
        if (dictionaryRepository != null) {
            val dbPegs = dictionaryRepository.getPegsByCode(targetCode)
            for (peg in dbPegs) {
                val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(peg.word, ruleSet)
                if (enc.isValid && enc.code == targetCode) {
                    collectedPegs.add(peg.copy(code = enc.code, normalizedWord = enc.normalizedWord))
                }
            }
        }

        // 1B. Check in-memory validated V2.3 SeedDictionary
        val seedMatches = SeedDictionary.getValidatedSeedPegs(ruleSet).filter { it.code == targetCode }
        for (seedPeg in seedMatches) {
            if (collectedPegs.none { it.normalizedWord == seedPeg.normalizedWord }) {
                collectedPegs.add(seedPeg)
                if (dictionaryRepository != null) {
                    runCatching {
                        dictionaryRepository?.insertOrUpdatePeg(
                            peg = seedPeg,
                            expectedCode = targetCode,
                            allowOverwriteApproved = false
                        )
                    }
                }
            }
        }

        // 1C. Ask Gemini AI to build 1 single meaningful Hindi word for the entire targetCode using its available alphabets & matras
        val hasApprovedAGrade = collectedPegs.any { it.qualityGrade == "A" && it.verificationStatus == "approved" }
        if (aiProvider != null && (invokeAi || !hasApprovedAGrade)) {
            usedAi = aiProvider.isConfigured
            val discovered = aiProvider.discoverCandidatesForCode(
                code = targetCode,
                count = 6,
                onRejectedAudit = { audits ->
                    rejectedAudits.addAll(audits)
                }
            )
            for (cand in discovered) {
                val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(cand.word, ruleSet)
                if (enc.isValid && enc.code == targetCode) {
                    if (collectedPegs.none { it.normalizedWord == enc.normalizedWord }) {
                        collectedPegs.add(cand)
                        if (dictionaryRepository != null) {
                            runCatching {
                                dictionaryRepository?.insertOrUpdatePeg(
                                    peg = cand,
                                    expectedCode = targetCode,
                                    allowOverwriteApproved = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rank 1-word candidates
        val rankedCandidates = rankAndFormatCandidates(
            enteredNumber = targetCode,
            pegs = collectedPegs,
            ruleSet = ruleSet
        )
        val bestSingleWord = rankedCandidates.firstOrNull()

        val segmented: SegmentedNumberBreakdown?
        val resolvedWordCount: Int

        if (bestSingleWord != null) {
            // k = 1 SUCCEEDED!
            progressiveAttempts.add(
                WordCountAttemptStep(
                    wordCount = 1,
                    succeeded = true,
                    matchedSplits = listOf(targetCode),
                    matchedWords = listOf(bestSingleWord.hindiWord),
                    summaryHi = "✓ प्रयास 1 (1 शब्द): पूरी संख्या '$targetCode' के लिए एकल सार्थक शब्द '${bestSingleWord.hindiWord}' (कोड ${bestSingleWord.finalEncodedNumber}) सफलतापूर्वक मिल गया!"
                )
            )
            resolvedWordCount = 1
            // Only build a multi-word breakdown as an optional secondary reference if the single word is not Grade A and length >= 3
            segmented = if (targetCode.length >= 3 && bestSingleWord.qualityGrade != "A") {
                buildProgressiveMultiWordBreakdown(
                    targetCode = targetCode,
                    ruleSet = ruleSet,
                    invokeAi = false,
                    priorAttempts = progressiveAttempts.toMutableList()
                )
            } else {
                null
            }
        } else {
            // k = 1 FAILED -> Now progressively try k = 2 words, then k = 3 words, and so on!
            progressiveAttempts.add(
                WordCountAttemptStep(
                    wordCount = 1,
                    succeeded = false,
                    matchedSplits = listOf(targetCode),
                    matchedWords = emptyList(),
                    summaryHi = "✕ प्रयास 1 (1 शब्द): पूरी संख्या '$targetCode' के लिए कोई एकल सार्थक शब्द नहीं मिला → अब 2 शब्दों में बनाने का प्रयास..."
                )
            )
            if (targetCode.length >= 2) {
                val multiBreakdown = buildProgressiveMultiWordBreakdown(
                    targetCode = targetCode,
                    ruleSet = ruleSet,
                    invokeAi = invokeAi,
                    priorAttempts = progressiveAttempts
                )
                segmented = multiBreakdown
                resolvedWordCount = multiBreakdown.wordCountUsed
            } else {
                segmented = null
                resolvedWordCount = 0
            }
        }

        val statusBanner = when {
            bestSingleWord != null && bestSingleWord.qualityGrade == "A" ->
                "✅ 1 शब्द में सफल: संख्या '$targetCode' के लिए सार्थक हिंदी शब्द '${bestSingleWord.hindiWord}' (कोड = ${bestSingleWord.finalEncodedNumber}) सत्यापित हुआ।"
            bestSingleWord != null ->
                "✅ 1 शब्द में सफल: संख्या '$targetCode' के लिए वैध हिंदी शब्द '${bestSingleWord.hindiWord}' (कोड = ${bestSingleWord.finalEncodedNumber}, स्तर: ${bestSingleWord.qualityGrade}) मिला।"
            segmented != null ->
                "✅ ${segmented.wordCountUsed} शब्दों में सफल: 1 एकल शब्द न मिलने पर संख्या '$targetCode' के लिए ${segmented.wordCountUsed} सार्थक शब्द (${segmented.combinedWordChain}) बनाए गए।"
            else ->
                "⚠️ संख्या '$targetCode' के लिए कोई मान्य शब्द नहीं मिला।"
        }

        return NumberGenerationResponse(
            enteredNumber = targetCode,
            bestCandidate = bestSingleWord,
            allCandidates = rankedCandidates,
            rejectedAiCandidates = rejectedAudits.distinctBy { it.candidateWord },
            segmentedBreakdown = segmented,
            progressiveAttempts = progressiveAttempts,
            resolvedWordCount = resolvedWordCount,
            digitConstructionGuide = PhoneticMatraEncoder.explainDigitConstruction(targetCode),
            usedAiDiscovery = usedAi,
            statusBannerHi = statusBanner
        )
    }

    /**
     * Progressively tries k = 2 words, then k = 3 words, then k = 4 words ...
     * stopping at the SMALLEST word count k where every part maps to a valid, rule-compliant Hindi word.
     */
    suspend fun buildProgressiveMultiWordBreakdown(
        targetCode: String,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3,
        invokeAi: Boolean = false,
        priorAttempts: MutableList<WordCountAttemptStep> = mutableListOf()
    ): SegmentedNumberBreakdown {
        val len = targetCode.length
        var winningItems: List<SegmentPegItem>? = null
        var winningK = len

        for (k in 2..len) {
            val candidatePartitions = generatePartitionsOfSizeK(
                code = targetCode,
                k = k,
                maxPartLength = 5
            )
            if (candidatePartitions.isEmpty()) continue

            // Evaluate all partitions of size k against existing dictionary & seed words
            data class PartitionEvaluation(
                val parts: List<String>,
                val resolvedPegs: List<PegWord?>,
                val matchedCount: Int,
                val totalScore: Int
            )

            val evaluations = candidatePartitions.map { parts ->
                val pegs = parts.map { partCode -> findBestPegForChunk(partCode, ruleSet) }
                val matchedCount = pegs.count { it != null }
                val idealLen = len.toDouble() / k.toDouble()
                val balancePenalty = parts.sumOf { p -> (abs(p.length - idealLen) * 15).toInt() }
                val qualityScore = pegs.filterNotNull().sumOf { peg ->
                    when (peg.qualityGrade.uppercase()) {
                        "A" -> 300
                        "B" -> 180
                        else -> 100
                    }
                }
                PartitionEvaluation(
                    parts = parts,
                    resolvedPegs = pegs,
                    matchedCount = matchedCount,
                    totalScore = qualityScore - balancePenalty
                )
            }.sortedWith(
                compareByDescending<PartitionEvaluation> { it.matchedCount }
                    .thenByDescending { it.totalScore }
            )

            // 1. Check if the top partition already has valid single words for ALL k parts
            val fullyMatched = evaluations.firstOrNull { it.matchedCount == k }
            if (fullyMatched != null) {
                winningItems = fullyMatched.parts.mapIndexed { idx, partCode ->
                    val peg = fullyMatched.resolvedPegs[idx]!!
                    val enc = PhoneticMatraEncoder.encode(peg.word, ruleSet)
                    SegmentPegItem(
                        segmentCode = partCode,
                        word = peg.word,
                        meaningHi = peg.meaningHi,
                        traceSummary = enc.formattedTraceSummary(),
                        qualityGrade = peg.qualityGrade,
                        verificationStatus = peg.verificationStatus,
                        pegWord = peg
                    )
                }
                winningK = k
                priorAttempts.add(
                    WordCountAttemptStep(
                        wordCount = k,
                        succeeded = true,
                        matchedSplits = fullyMatched.parts,
                        matchedWords = winningItems.map { it.word },
                        summaryHi = "✓ प्रयास $k ($k शब्द): संख्या '$targetCode' को $k भागों (${fullyMatched.parts.joinToString(" + ")}) में बाँटकर सार्थक शब्द '${winningItems.joinToString(" + ") { it.word }}' सफलतापूर्वक बन गए!"
                    )
                )
                break
            }

            // 2. If not all k parts matched locally, ask Gemini AI to build words for the missing part(s) of the best partition of size k
            if (aiProvider != null && (invokeAi || aiProvider.isConfigured)) {
                val bestCandidateEval = evaluations.firstOrNull { (k - it.matchedCount) <= 2 }
                if (bestCandidateEval != null) {
                    val resolvedAfterAi = bestCandidateEval.parts.mapIndexed { idx, partCode ->
                        val existing = bestCandidateEval.resolvedPegs[idx]
                        if (existing != null) {
                            existing
                        } else if (partCode.length <= 4) {
                            val aiFound = aiProvider.discoverCandidatesForCode(partCode, count = 4)
                                .firstOrNull { cand ->
                                    val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(cand.word, ruleSet)
                                    enc.isValid && enc.code == partCode
                                }
                            if (aiFound != null && dictionaryRepository != null) {
                                runCatching {
                                    dictionaryRepository?.insertOrUpdatePeg(
                                        peg = aiFound,
                                        expectedCode = partCode,
                                        allowOverwriteApproved = false
                                    )
                                }
                            }
                            aiFound
                        } else {
                            null
                        }
                    }

                    if (resolvedAfterAi.all { it != null }) {
                        winningItems = bestCandidateEval.parts.mapIndexed { idx, partCode ->
                            val peg = resolvedAfterAi[idx]!!
                            val enc = PhoneticMatraEncoder.encode(peg.word, ruleSet)
                            SegmentPegItem(
                                segmentCode = partCode,
                                word = peg.word,
                                meaningHi = peg.meaningHi,
                                traceSummary = enc.formattedTraceSummary(),
                                qualityGrade = peg.qualityGrade,
                                verificationStatus = peg.verificationStatus,
                                pegWord = peg
                            )
                        }
                        winningK = k
                        priorAttempts.add(
                            WordCountAttemptStep(
                                wordCount = k,
                                succeeded = true,
                                matchedSplits = bestCandidateEval.parts,
                                matchedWords = winningItems.map { it.word },
                                summaryHi = "✓ प्रयास $k ($k शब्द): AI और V2.3 नियमों की मदद से संख्या '$targetCode' को $k भागों (${bestCandidateEval.parts.joinToString(" + ")}) में बाँटकर '${winningItems.joinToString(" + ") { it.word }}' बनाए गए!"
                            )
                        )
                        break
                    }
                }
            }

            // Step k failed -> record and try k + 1 words!
            if (k < len) {
                priorAttempts.add(
                    WordCountAttemptStep(
                        wordCount = k,
                        succeeded = false,
                        matchedSplits = emptyList(),
                        matchedWords = emptyList(),
                        summaryHi = "✕ प्रयास $k ($k शब्द): $k सार्थक शब्दों में पूर्ण मिलान नहीं मिला → अब ${k + 1} शब्दों में प्रयास..."
                    )
                )
            }
        }

        // Fallback to 1-digit pegs if somehow even k == len wasn't set above
        val finalSegments = winningItems ?: targetCode.map { d ->
            val chunk = d.toString()
            val peg = findBestPegForChunk(chunk, ruleSet)
            if (peg != null) {
                val enc = PhoneticMatraEncoder.encode(peg.word, ruleSet)
                SegmentPegItem(
                    segmentCode = chunk,
                    word = peg.word,
                    meaningHi = peg.meaningHi,
                    traceSummary = enc.formattedTraceSummary(),
                    qualityGrade = peg.qualityGrade,
                    verificationStatus = peg.verificationStatus,
                    pegWord = peg
                )
            } else {
                SegmentPegItem(
                    segmentCode = chunk,
                    word = chunk,
                    meaningHi = "अंक $chunk",
                    traceSummary = chunk,
                    qualityGrade = "Unverified",
                    verificationStatus = "unverified",
                    pegWord = null
                )
            }
        }

        val chainStr = finalSegments.joinToString(" + ") { "${it.word} (${it.segmentCode})" }
        val traceStr = finalSegments.joinToString("  │  ") { "[${it.segmentCode}] ${it.word}: ${it.traceSummary}" }
        val storyHi = buildCombinedSegmentStory(targetCode, finalSegments)
        val explanationHi = "पहले 1 शब्द बनाने का प्रयास किया गया; जब 1 एकल सार्थक शब्द नहीं मिला, तब न्यूनतम $winningK सार्थक शब्दों (${finalSegments.joinToString(" + ") { it.segmentCode }}) में '$chainStr' तैयार किया गया है।"

        return SegmentedNumberBreakdown(
            enteredNumber = targetCode,
            wordCountUsed = finalSegments.size,
            attemptSteps = priorAttempts,
            segments = finalSegments,
            combinedWordChain = chainStr,
            combinedEncodingTrace = traceStr,
            combinedVividStoryHi = storyHi,
            whySegmentedExplanationHi = explanationHi
        )
    }

    /**
     * Generates all ways to split [code] into exactly [k] contiguous non-empty parts,
     * where each part has length at most [maxPartLength].
     */
    internal fun generatePartitionsOfSizeK(
        code: String,
        k: Int,
        maxPartLength: Int = 5
    ): List<List<String>> {
        if (k <= 0 || code.length < k) return emptyList()
        if (k == 1) {
            return if (code.length <= maxPartLength || maxPartLength <= 0) listOf(listOf(code)) else emptyList()
        }
        val results = mutableListOf<List<String>>()
        val maxFirstLen = (code.length - (k - 1)).coerceAtMost(maxPartLength)
        for (len in 1..maxFirstLen) {
            val firstPart = code.substring(0, len)
            val remaining = code.substring(len)
            val subPartitions = generatePartitionsOfSizeK(remaining, k - 1, maxPartLength)
            for (sub in subPartitions) {
                results.add(listOf(firstPart) + sub)
            }
        }
        return results
    }

    /**
     * Deterministically validates, enriches with all 9 required fields, and ranks candidates.
     */
    fun rankAndFormatCandidates(
        enteredNumber: String,
        pegs: List<PegWord>,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3
    ): List<GeneratedWordCandidate> {
        val validFormatted = mutableListOf<GeneratedWordCandidate>()
        val seenWords = mutableSetOf<String>()

        for (peg in pegs) {
            val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(peg.word, ruleSet)
            if (!enc.isValid || enc.code != enteredNumber) continue
            if (!seenWords.add(enc.normalizedWord)) continue

            val meaningHi = peg.meaningHi.takeIf { it.isNotBlank() }
                ?: "${enc.normalizedWord} (सार्थक हिंदी शब्द)"
            val whyGoodPeg = peg.dictionaryEvidence?.takeIf { it.isNotBlank() }
                ?: SeedDictionary.buildWhyGoodPeg(
                    word = enc.normalizedWord,
                    code = enc.code,
                    category = peg.category,
                    meaningHi = meaningHi,
                    qualityGrade = peg.qualityGrade
                )
            val vividStory = peg.mnemonicStory?.takeIf { it.isNotBlank() }
                ?: SeedDictionary.buildVividMnemonicStory(
                    word = enc.normalizedWord,
                    code = enc.code,
                    meaningHi = meaningHi,
                    visualAssociation = peg.visualAssociation
                )
            val normalizedGrade = when (peg.qualityGrade.uppercase()) {
                "A" -> "A"
                "B" -> "B"
                else -> "Unverified"
            }
            val score = computeCandidateScore(peg, normalizedGrade, meaningHi, vividStory)

            val enrichedPeg = peg.copy(
                word = enc.normalizedWord,
                normalizedWord = enc.normalizedWord,
                code = enc.code,
                meaningHi = meaningHi,
                qualityGrade = normalizedGrade,
                encodingBreakdown = enc.formattedTraceSummary(),
                mnemonicStory = vividStory,
                dictionaryEvidence = whyGoodPeg
            )

            validFormatted.add(
                GeneratedWordCandidate(
                    enteredNumber = enteredNumber,
                    hindiWord = enc.normalizedWord,
                    meaningHi = meaningHi,
                    meaningEn = peg.meaningEn,
                    characterByCharacterEncoding = enc.formattedTraceSummary(),
                    traces = enc.traces,
                    finalEncodedNumber = enc.code,
                    whyGoodMnemonicPeg = whyGoodPeg,
                    vividMentalStory = vividStory,
                    qualityGrade = normalizedGrade,
                    verificationStatus = peg.verificationStatus,
                    verificationSource = peg.verificationSource,
                    pegWord = enrichedPeg,
                    rankScore = score
                )
            )
        }

        return validFormatted.sortedWith(
            compareByDescending<GeneratedWordCandidate> { it.rankScore }
                .thenBy { it.hindiWord.length }
                .thenBy { it.hindiWord }
        )
    }

    private fun computeCandidateScore(
        peg: PegWord,
        normalizedGrade: String,
        meaningHi: String,
        vividStory: String
    ): Int {
        var score = 0
        score += when (normalizedGrade) {
            "A" -> 300
            "B" -> 200
            else -> 100
        }
        score += when (peg.verificationStatus.lowercase()) {
            "approved" -> 60
            "unverified" -> 20
            else -> 0
        }
        score += when (peg.commonness?.lowercase()) {
            "high" -> 35
            "medium" -> 20
            "low" -> 5
            else -> 15
        }
        if (peg.category.contains("संज्ञा")) {
            score += 25
        }
        if (peg.verificationSource == "v2_3_seed" || peg.verificationSource == "user_verified") {
            score += 20
        }
        if (meaningHi.length > 5 && !meaningHi.endsWith("का अर्थ")) {
            score += 15
        }
        if (vividStory.isNotBlank()) {
            score += 10
        }
        if (peg.isFavorite) {
            score += 15
        }
        return score
    }

    private suspend fun findBestPegForChunk(
        chunk: String,
        ruleSet: EncodingRuleSet
    ): PegWord? {
        val dbCandidates = dictionaryRepository?.getPegsByCode(chunk).orEmpty()
        val seedCandidates = SeedDictionary.getValidatedSeedPegs(ruleSet).filter { it.code == chunk }
        val all = (dbCandidates + seedCandidates).distinctBy { it.normalizedWord }
        val ranked = rankAndFormatCandidates(chunk, all, ruleSet)
        return ranked.firstOrNull()?.pegWord
    }

    private fun buildCombinedSegmentStory(
        enteredNumber: String,
        segments: List<SegmentPegItem>
    ): String {
        if (segments.isEmpty()) return ""
        when (enteredNumber) {
            "1947" -> return "शांत नीले सरोवर में खिले एक विशाल गुलाबी 'कमल' (19) से सुनहरा मीठा 'रस' (47) टपक रहा है — 1947 की स्मृति का यह दृश्य कभी नहीं भूलता!"
            "2026" -> return "सूरज की रोशनी में उड़ती सुनहरी 'धूल' (20) के बादल में एक जादुई महल अचानक आँखों से 'ओझल' (26) हो गया — यही 2026 का जीवंत दृश्य है!"
            "1857" -> return "प्राचीन 'काल' (18) के समय के दरवाज़े से एक चमकता हुआ सफेद 'हंस' (57) उड़ान भर रहा है (18 + 57 = 1857)।"
            "1526" -> return "गर्म भाप के धुएँ में चाय का सुनहरा 'कप' (15) अचानक आँखों से 'ओझल' (26) हो गया (15 + 26 = 1526)।"
        }

        val sb = StringBuilder()
        sb.append("कल्पना करें (संख्या $enteredNumber — ${segments.size} शब्द): ")
        segments.forEachIndexed { idx, seg ->
            when (idx) {
                0 -> sb.append("सबसे पहले आपके सामने एक विशाल और चमकदार '${seg.word}' (${seg.meaningHi}, कोड ${seg.segmentCode}) दिखाई देता है। ")
                segments.lastIndex -> sb.append("अंत में उसके भीतर से '${seg.word}' (${seg.meaningHi}, कोड ${seg.segmentCode}) प्रकट होकर पूरे दृश्य को जोड़ देता है!")
                else -> sb.append("तभी उससे जुड़कर '${seg.word}' (${seg.meaningHi}, कोड ${seg.segmentCode}) उभरता है, ")
            }
        }
        return sb.toString()
    }
}
