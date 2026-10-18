package com.pocketdev.ank_shabd.domain.encoder

import java.text.Normalizer

object PhoneticMatraEncoder {

    const val ENCODER_VERSION = "2.3"

    /**
     * Encodes a Hindi word into a deterministic number code based on the given ruleset.
     * Preserves leading zeroes as String.
     * Generates a component-by-component trace explaining every emitted or skipped digit.
     */
    fun encode(word: String, ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3): EncodingResult {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) {
            return EncodingResult(
                originalWord = word,
                normalizedWord = "",
                code = "",
                traces = emptyList(),
                isValid = true,
                warnings = emptyList(),
                ruleSetId = ruleSet.id
            )
        }

        // Normalize Devanagari text to NFC
        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFC)
        val codeBuilder = StringBuilder()
        val traces = mutableListOf<ComponentTrace>()
        val warnings = mutableListOf<String>()

        var lastComponentType: ComponentType? = null
        var i = 0
        while (i < normalized.length) {
            val currentChar = normalized[i]

            // Check for zero-width joiner / non-joiner
            if (currentChar == '\u200C' || currentChar == '\u200D') {
                i++
                continue
            }

            // Check for spaces or punctuation between words
            if (currentChar.isWhitespace() || currentChar == '-' || currentChar == '–') {
                traces.add(
                    ComponentTrace(
                        character = currentChar.toString(),
                        componentType = ComponentType.SPACE_OR_PUNCTUATION,
                        digitEmitted = null,
                        ruleApplied = "विभाजक (कोई अंक नहीं)",
                        position = i
                    )
                )
                lastComponentType = ComponentType.SPACE_OR_PUNCTUATION
                i++
                continue
            }

            // Look ahead for combining Nukta (\u093C)
            val isFollowedByNukta = (i + 1 < normalized.length) && (normalized[i + 1] == '\u093C')
            val rawToken = if (isFollowedByNukta) {
                normalized.substring(i, i + 2)
            } else {
                currentChar.toString()
            }
            val stepSize = if (isFollowedByNukta) 2 else 1

            // Canonicalize nukta representation: either decomposed (क + ़) or precomposed (क़)
            val token = canonicalizeNukta(rawToken)

            // 1. Carrier consonants: य, व, ह, ल
            if (ruleSet.carrierConsonants.contains(token)) {
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.CARRIER_CONSONANT,
                        digitEmitted = null,
                        ruleApplied = "$token → वाहक व्यंजन (अंक लोप, मात्रा नियम सुरक्षित)",
                        position = i
                    )
                )
                lastComponentType = ComponentType.CARRIER_CONSONANT
            }
            // 2. Mapped Consonants (including nukta consonants)
            else if (ruleSet.consonantMapping.containsKey(token)) {
                val digit = ruleSet.consonantMapping[token]!!
                codeBuilder.append(digit)
                val desc = getConsonantGroupDescription(digit)
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.CONSONANT,
                        digitEmitted = digit,
                        ruleApplied = "$token → $digit ($desc)",
                        position = i
                    )
                )
                lastComponentType = ComponentType.CONSONANT
            }
            // 3. Matras (dependent vowel signs attached to consonants/carriers)
            else if (ruleSet.matraMapping.containsKey(token)) {
                val digit = ruleSet.matraMapping[token]!!
                val validAttachment = lastComponentType == ComponentType.CONSONANT ||
                    lastComponentType == ComponentType.CARRIER_CONSONANT
                if (!validAttachment) {
                    val warn = "अमान्य मात्रा स्थान '$token' स्थान $i पर (बिना आधार व्यंजन के मात्रा)"
                    warnings.add(warn)
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.MATRA,
                            digitEmitted = digit,
                            ruleApplied = "$token → अमान्य मात्रा क्रम",
                            position = i,
                            isWarning = true,
                            warningMessage = warn
                        )
                    )
                } else {
                    val desc = getMatraDescription(token, digit)
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.MATRA,
                            digitEmitted = digit,
                            ruleApplied = "$token → $digit ($desc)",
                            position = i
                        )
                    )
                }
                codeBuilder.append(digit)
                lastComponentType = ComponentType.MATRA
            }
            // 4. Independent Vowels (उ, ऊ, ए, ऐ, ओ, औ, इ, ई, ऋ, आ)
            else if (ruleSet.independentVowelMapping.containsKey(token)) {
                val digit = ruleSet.independentVowelMapping[token]!!
                codeBuilder.append(digit)
                val desc = getIndependentVowelDescription(token, digit)
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.INDEPENDENT_VOWEL,
                        digitEmitted = digit,
                        ruleApplied = "$token → $digit (स्वतंत्र स्वर $desc)",
                        position = i
                    )
                )
                lastComponentType = ComponentType.INDEPENDENT_VOWEL
            }
            // 5. Diacritics: Anusvara (ं = 5) and Visarga (ः = 6)
            else if (ruleSet.diacriticMapping.containsKey(token)) {
                val digit = ruleSet.diacriticMapping[token]!!
                val validAttachment = lastComponentType == ComponentType.CONSONANT ||
                    lastComponentType == ComponentType.CARRIER_CONSONANT ||
                    lastComponentType == ComponentType.INDEPENDENT_VOWEL ||
                    lastComponentType == ComponentType.MATRA
                val desc = if (token == "ं") "अनुस्वार" else "विसर्ग"
                if (!validAttachment) {
                    val warn = "अमान्य चिह्न '$token' ($desc) स्थान $i पर (बिना स्वर/व्यंजन आधार के)"
                    warnings.add(warn)
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.DIACRITIC,
                            digitEmitted = digit,
                            ruleApplied = "$token ($desc) → अमान्य स्थान",
                            position = i,
                            isWarning = true,
                            warningMessage = warn
                        )
                    )
                } else {
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.DIACRITIC,
                            digitEmitted = digit,
                            ruleApplied = "$token ($desc) → $digit",
                            position = i
                        )
                    )
                }
                codeBuilder.append(digit)
                lastComponentType = ComponentType.DIACRITIC
            }
            // 6. Zero-digit signs: Chandrabindu (ँ), independent 'अ', Halant (्), standalone Nukta (़)
            else if (token == "ँ") {
                val validAttachment = lastComponentType == ComponentType.CONSONANT ||
                    lastComponentType == ComponentType.CARRIER_CONSONANT ||
                    lastComponentType == ComponentType.INDEPENDENT_VOWEL ||
                    lastComponentType == ComponentType.MATRA
                if (!validAttachment) {
                    val warn = "अमान्य चंद्रबिंदु 'ँ' स्थान $i पर (बिना आधार वर्ण के)"
                    warnings.add(warn)
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.DIACRITIC,
                            digitEmitted = null,
                            ruleApplied = "ँ → अमान्य स्थान",
                            position = i,
                            isWarning = true,
                            warningMessage = warn
                        )
                    )
                } else {
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.DIACRITIC,
                            digitEmitted = null,
                            ruleApplied = "ँ (चंद्रबिंदु) → V2.3 नियम: कोई अंक नहीं",
                            position = i
                        )
                    )
                }
                lastComponentType = ComponentType.DIACRITIC
            } else if (token == "अ") {
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.INDEPENDENT_VOWEL,
                        digitEmitted = null,
                        ruleApplied = "अ (स्वतंत्र अ) → कोई अंक नहीं (अंतर्निहित स्वर समान)",
                        position = i
                    )
                )
                lastComponentType = ComponentType.INDEPENDENT_VOWEL
            } else if (token == "्") {
                val validAttachment = lastComponentType == ComponentType.CONSONANT ||
                    lastComponentType == ComponentType.CARRIER_CONSONANT
                if (!validAttachment) {
                    val warn = "अमान्य हलंत '्' स्थान $i पर (केवल व्यंजन के बाद मान्य)"
                    warnings.add(warn)
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.VIRAMA,
                            digitEmitted = null,
                            ruleApplied = "् (हलंत) → अमान्य स्थान",
                            position = i,
                            isWarning = true,
                            warningMessage = warn
                        )
                    )
                } else {
                    traces.add(
                        ComponentTrace(
                            character = token,
                            componentType = ComponentType.VIRAMA,
                            digitEmitted = null,
                            ruleApplied = "् (हलंत) → स्वर लोप, कोई अंक नहीं",
                            position = i
                        )
                    )
                }
                lastComponentType = ComponentType.VIRAMA
            } else if (token == "़") {
                val warn = "असमर्थित या स्वतंत्र नुक्ता चिह्न '़' स्थान $i पर मिला"
                warnings.add(warn)
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.NUKTA,
                        digitEmitted = null,
                        ruleApplied = "़ (नुक्ता) → असमर्थित नुक्ता प्रयोग",
                        position = i,
                        isWarning = true,
                        warningMessage = warn
                    )
                )
                lastComponentType = ComponentType.NUKTA
            }
            // 7. Explicit Unsupported Symbols (ऑ, ॉ, ऍ, ॅ, etc.)
            else if (ruleSet.unsupportedSymbols.contains(token)) {
                val warn = "असमर्थित चिह्न/स्वर '$token' स्थान $i पर मिला (समीक्षा अपेक्षित)"
                warnings.add(warn)
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.UNSUPPORTED,
                        digitEmitted = null,
                        ruleApplied = "$token → असमर्थित चिह्न (कोई अंक नहीं निकला)",
                        position = i,
                        isWarning = true,
                        warningMessage = warn
                    )
                )
                lastComponentType = ComponentType.UNSUPPORTED
            }
            // 8. Unknown / Non-Devanagari characters (English, unsupported symbols, unlisted consonants)
            else {
                val warn = "अज्ञात या गैर-देवनागरी चिह्न '$token' स्थान $i पर मिला"
                warnings.add(warn)
                traces.add(
                    ComponentTrace(
                        character = token,
                        componentType = ComponentType.UNSUPPORTED,
                        digitEmitted = null,
                        ruleApplied = "$token → अज्ञात/अमान्य वर्ण",
                        position = i,
                        isWarning = true,
                        warningMessage = warn
                    )
                )
                lastComponentType = ComponentType.UNSUPPORTED
            }

            i += stepSize
        }

        return EncodingResult(
            originalWord = word,
            normalizedWord = normalized,
            code = codeBuilder.toString(),
            traces = traces,
            isValid = warnings.isEmpty(),
            warnings = warnings,
            ruleSetId = ruleSet.id
        )
    }

    /**
     * Strictly encodes and validates a candidate intended as a single Hindi peg word.
     * Rejects empty strings, multi-word phrases (spaces/hyphens), and words without base letters.
     */
    fun encodeSingleWordCandidate(
        word: String,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3
    ): EncodingResult {
        val baseResult = encode(word, ruleSet)
        val extraWarnings = baseResult.warnings.toMutableList()

        if (baseResult.normalizedWord.isEmpty()) {
            extraWarnings.add("उम्मीदवार शब्द खाली है।")
        }
        if (baseResult.traces.any { it.componentType == ComponentType.SPACE_OR_PUNCTUATION }) {
            extraWarnings.add("एकल पेग शब्द में स्पेस, हाइफ़न या एक से अधिक शब्द नहीं हो सकते।")
        }
        val hasBaseLetter = baseResult.traces.any {
            it.componentType == ComponentType.CONSONANT ||
                it.componentType == ComponentType.CARRIER_CONSONANT ||
                it.componentType == ComponentType.INDEPENDENT_VOWEL
        }
        if (baseResult.normalizedWord.isNotEmpty() && !hasBaseLetter) {
            extraWarnings.add("शब्द में कोई मान्य हिंदी स्वर या व्यंजन नहीं है।")
        }

        return if (extraWarnings.size == baseResult.warnings.size) {
            baseResult
        } else {
            baseResult.copy(
                isValid = false,
                warnings = extraWarnings
            )
        }
    }

    /**
     * Generates a human- and LLM-readable breakdown of which Devanagari components
     * can produce each digit of [targetCode] under V2.3 rules.
     */
    fun explainDigitConstruction(targetCode: String): String {
        if (targetCode.isBlank()) return ""
        val palette = buildNumberPalette(targetCode)
        val lines = palette.digitBlocks.map { block ->
            val consText = "व्यंजन [${block.availableConsonants.joinToString(", ")}]"
            val matraText = if (block.availableMatras.isNotEmpty()) {
                " | मात्रा/चिह्न [${block.availableMatras.joinToString(", ")}]"
            } else {
                " | (कोई मात्रा नहीं)"
            }
            val vowelText = if (block.availableIndependentVowels.isNotEmpty()) {
                " | स्वतंत्र स्वर [${block.availableIndependentVowels.joinToString(", ")}]"
            } else {
                ""
            }
            val carrierText = if (block.carrierMatraExamples.isNotEmpty()) {
                " | वाहक+मात्रा [${block.carrierMatraExamples.joinToString(", ")}]"
            } else {
                ""
            }
            "अंक #${block.position} ('${block.digit}'): $consText$matraText$vowelText$carrierText"
        }.toMutableList()
        if (palette.readySyllableHints.isNotEmpty()) {
            lines.add("संयुक्त अक्षर+मात्रा संकेत (2-3 अंक एक साथ): ${palette.readySyllableHints.joinToString(" | ")}")
        }
        lines.add("निःशुल्क वाहक अक्षर (कोई अंक नहीं): य, व, ह, ल | निःशुल्क स्वर/चिह्न: अ (सामान्य स्वर), ँ (चंद्रबिंदु)")
        return lines.joinToString("\n")
    }

    /**
     * Builds the complete V2.3 palette of available alphabets (consonants), matras,
     * independent vowels, carrier-matra syllables, and combined syllable hints for [targetCode].
     */
    fun buildNumberPalette(targetCode: String): NumberBuildingBlocksPalette {
        val clean = targetCode.filter { it in '0'..'9' }
        val blocks = clean.mapIndexed { idx, ch ->
            val d = ch.toString()
            when (d) {
                "0" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "0",
                    availableConsonants = listOf("श", "ष", "ज़"),
                    availableMatras = listOf("ु (उ मात्रा)", "ू (ऊ मात्रा)"),
                    rawMatras = listOf("ु", "ू"),
                    availableIndependentVowels = listOf("उ", "ऊ"),
                    carrierMatraExamples = listOf("यु", "यू", "वु", "वू", "हु", "हू", "लु", "लू"),
                    usageHintHi = "व्यंजन (श, ष, ज़) लें या पिछले अक्षर पर ु / ू मात्रा लगाएँ"
                )
                "1" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "1",
                    availableConsonants = listOf("क", "ख", "ग", "घ", "क़", "ख़", "ग़"),
                    availableMatras = listOf("े (ए मात्रा)", "ै (ऐ मात्रा)"),
                    rawMatras = listOf("े", "ै"),
                    availableIndependentVowels = listOf("ए", "ऐ"),
                    carrierMatraExamples = listOf("ये", "वे", "हे", "ले", "है", "वै"),
                    usageHintHi = "व्यंजन (क, ख, ग, घ) लें या पिछले अक्षर पर े / ै मात्रा लगाएँ"
                )
                "2" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "2",
                    availableConsonants = listOf("द", "ध", "ड", "ढ", "ड़", "ढ़"),
                    availableMatras = listOf("ो (ओ मात्रा)", "ौ (औ मात्रा)"),
                    rawMatras = listOf("ो", "ौ"),
                    availableIndependentVowels = listOf("ओ", "औ"),
                    carrierMatraExamples = listOf("यो", "वो", "हो", "लो", "हौ", "लौ"),
                    usageHintHi = "व्यंजन (द, ध, ड, ढ, ड़, ढ़) लें या पिछले अक्षर पर ो / ौ मात्रा लगाएँ"
                )
                "3" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "3",
                    availableConsonants = listOf("त", "थ", "ट", "ठ"),
                    availableMatras = listOf("ि (इ मात्रा)", "ी (ई मात्रा)"),
                    rawMatras = listOf("ि", "ी"),
                    availableIndependentVowels = listOf("इ", "ई"),
                    carrierMatraExamples = listOf("यि", "यी", "वि", "वी", "हि", "ही", "लि", "ली"),
                    usageHintHi = "व्यंजन (त, थ, ट, ठ) लें या पिछले अक्षर पर ि / ी मात्रा लगाएँ"
                )
                "4" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "4",
                    availableConsonants = listOf("च", "ज", "र"),
                    availableMatras = listOf("ृ (ऋ मात्रा)"),
                    rawMatras = listOf("ृ"),
                    availableIndependentVowels = listOf("ऋ"),
                    carrierMatraExamples = listOf("हृ", "वृ"),
                    usageHintHi = "व्यंजन (च, ज, र) लें या पिछले अक्षर पर ृ मात्रा लगाएँ"
                )
                "5" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "5",
                    availableConsonants = listOf("प", "फ", "फ़"),
                    availableMatras = listOf("ं (अनुस्वार बिंदी)"),
                    rawMatras = listOf("ं"),
                    availableIndependentVowels = emptyList(),
                    carrierMatraExamples = listOf("हं", "वं", "लं", "यं"),
                    usageHintHi = "व्यंजन (प, फ, फ़) लें या पिछले अक्षर/मात्रा के बाद अनुस्वार (ं) लगाएँ"
                )
                "6" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "6",
                    availableConsonants = listOf("छ", "झ"),
                    availableMatras = listOf("ः (विसर्ग)"),
                    rawMatras = listOf("ः"),
                    availableIndependentVowels = emptyList(),
                    carrierMatraExamples = listOf("हः", "लः"),
                    usageHintHi = "व्यंजन (छ, झ) लें या विसर्ग (ः) लगाएँ"
                )
                "7" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "7",
                    availableConsonants = listOf("स"),
                    availableMatras = emptyList(),
                    rawMatras = emptyList(),
                    availableIndependentVowels = emptyList(),
                    carrierMatraExamples = emptyList(),
                    usageHintHi = "अंक 7 के लिए केवल व्यंजन 'स' उपलब्ध है (कोई मात्रा नहीं)"
                )
                "8" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "8",
                    availableConsonants = listOf("ब", "भ"),
                    availableMatras = listOf("ा (आ मात्रा)"),
                    rawMatras = listOf("ा"),
                    availableIndependentVowels = listOf("आ"),
                    carrierMatraExamples = listOf("या", "वा", "हा", "ला"),
                    usageHintHi = "व्यंजन (ब, भ) लें या पिछले अक्षर पर 'ा' (आ) मात्रा लगाएँ"
                )
                "9" -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = "9",
                    availableConsonants = listOf("न", "म", "ण"),
                    availableMatras = emptyList(),
                    rawMatras = emptyList(),
                    availableIndependentVowels = emptyList(),
                    carrierMatraExamples = emptyList(),
                    usageHintHi = "अंक 9 के लिए केवल व्यंजन (न, म, ण) उपलब्ध हैं (कोई मात्रा नहीं)"
                )
                else -> DigitBuildingBlocks(
                    position = idx + 1,
                    digit = d,
                    availableConsonants = emptyList(),
                    availableMatras = emptyList(),
                    rawMatras = emptyList(),
                    availableIndependentVowels = emptyList(),
                    carrierMatraExamples = emptyList(),
                    usageHintHi = ""
                )
            }
        }

        // Build ready-to-use multi-digit syllable hints by combining Consonant(d_i) + Matra(d_{i+1}) [+ Anusvara(d_{i+2} == 5)]
        val syllableHints = mutableListOf<String>()
        for (i in 0 until blocks.size - 1) {
            val first = blocks[i]
            val second = blocks[i + 1]
            if (second.rawMatras.isNotEmpty() && first.availableConsonants.isNotEmpty()) {
                val topConsonants = first.availableConsonants.take(3)
                val combos = topConsonants.flatMap { c ->
                    second.rawMatras.map { m -> "$c$m" }
                }
                syllableHints.add("\"${first.digit}${second.digit}\" → ${combos.joinToString(", ")}")
                if (i + 2 < blocks.size && blocks[i + 2].digit == "5" && second.digit != "5") {
                    val tripleCombos = topConsonants.take(2).map { c -> "${c}${second.rawMatras.first()}ं" }
                    syllableHints.add("\"${first.digit}${second.digit}5\" → ${tripleCombos.joinToString(", ")}")
                }
            }
        }

        return NumberBuildingBlocksPalette(
            targetNumber = clean,
            digitBlocks = blocks,
            carrierConsonants = listOf("य", "व", "ह", "ल"),
            zeroDigitHelpers = listOf("अ (सामान्य बिना मात्रा स्वर)", "ँ (चंद्रबिंदु)", "् (हलंत)"),
            readySyllableHints = syllableHints
        )
    }

    /**
     * Normalizes composite nukta character representations.
     */
    private fun canonicalizeNukta(token: String): String {
        return when (token) {
            "क़", "क\u093C", "\u0958" -> "क़"
            "ख़", "ख\u093C", "\u0959" -> "ख़"
            "ग़", "ग\u093C", "\u095A" -> "ग़"
            "ज़", "ज\u093C", "\u095B" -> "ज़"
            "ड़", "ड\u093C", "\u095C" -> "ड़"
            "ढ़", "ढ\u093C", "\u095D" -> "ढ़"
            "फ़", "फ\u093C", "\u095E" -> "फ़"
            else -> token
        }
    }

    private fun getConsonantGroupDescription(digit: String): String {
        return when (digit) {
            "0" -> "श/ष/ज़ समूह 0"
            "1" -> "क-वर्ग समूह 1"
            "2" -> "द/ध/ड समूह 2"
            "3" -> "त/ट समूह 3"
            "4" -> "च/ज/र समूह 4"
            "5" -> "प-वर्ग समूह 5"
            "6" -> "छ/झ समूह 6"
            "7" -> "स समूह 7"
            "8" -> "ब/भ समूह 8"
            "9" -> "न/म/ण समूह 9"
            else -> "समूह $digit"
        }
    }

    private fun getMatraDescription(token: String, digit: String): String {
        return when (token) {
            "ु", "ू" -> "उ/ऊ मात्रा 0"
            "े", "ै" -> "ए/ऐ मात्रा 1"
            "ो", "ौ" -> "ओ/औ मात्रा 2"
            "ि", "ी" -> "इ/ई मात्रा 3"
            "ृ" -> "ऋ मात्रा 4"
            "ा" -> "आ मात्रा 8"
            else -> "मात्रा $digit"
        }
    }

    private fun getIndependentVowelDescription(token: String, digit: String): String {
        return when (token) {
            "उ", "ऊ" -> "उ/ऊ = 0"
            "ए", "ऐ" -> "ए/ऐ = 1"
            "ओ", "औ" -> "ओ/औ = 2"
            "इ", "ई" -> "इ/ई = 3"
            "ऋ" -> "ऋ = 4"
            "आ" -> "आ = 8"
            else -> "$token = $digit"
        }
    }
}

data class DigitBuildingBlocks(
    val position: Int,
    val digit: String,
    val availableConsonants: List<String>,
    val availableMatras: List<String>,
    val rawMatras: List<String>,
    val availableIndependentVowels: List<String>,
    val carrierMatraExamples: List<String>,
    val usageHintHi: String
)

data class NumberBuildingBlocksPalette(
    val targetNumber: String,
    val digitBlocks: List<DigitBuildingBlocks>,
    val carrierConsonants: List<String>,
    val zeroDigitHelpers: List<String>,
    val readySyllableHints: List<String>
)

