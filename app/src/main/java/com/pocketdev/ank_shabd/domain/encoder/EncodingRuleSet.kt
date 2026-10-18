package com.pocketdev.ank_shabd.domain.encoder

data class EncodingRuleSet(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
    // Consonant to Digit mapping (e.g. 'क' -> "1", 'ज़' -> "0")
    val consonantMapping: Map<String, String>,
    // Carrier consonants that emit no consonant digit
    val carrierConsonants: Set<String>,
    // Independent vowels mapping (e.g. 'उ' -> "0", 'आ' -> "8")
    val independentVowelMapping: Map<String, String>,
    // Dependent vowel signs (matras) mapping (e.g. 'ु' -> "0", 'ा' -> "8")
    val matraMapping: Map<String, String>,
    // Diacritics mapping (e.g. 'ं' -> "5", 'ः' -> "6")
    val diacriticMapping: Map<String, String>,
    // Zero-digit signs that are formally acknowledged and skipped (e.g. 'ँ', 'अ', '्')
    val zeroDigitSigns: Set<String>,
    // Unsupported notation symbols that produce explicit warnings
    val unsupportedSymbols: Set<String>
) {
    val ruleSetId: String get() = id

    companion object {
        const val DEFAULT_RULESET_ID = "HINDI_PHONETIC_MATRA_V2_3"

        val V2_3 = EncodingRuleSet(
            id = DEFAULT_RULESET_ID,
            name = "हिंदी फोनेटिक एवं मात्रा नंबर सिस्टम V2.3",
            version = "2.3",
            description = "संख्या को वास्तविक हिंदी शब्द में बदलने की जाँच-योग्य प्रणाली (V2.3 मानक)",
            isDefault = true,
            isCustom = false,
            consonantMapping = mapOf(
                // 0: श, ष, ज़
                "श" to "0", "ष" to "0", "ज़" to "0",
                // 1: क, ख, ग, घ, क़, ख़, ग़
                "क" to "1", "ख" to "1", "ग" to "1", "घ" to "1",
                "क़" to "1", "ख़" to "1", "ग़" to "1",
                // 2: द, ध, ड, ढ, ड़, ढ़
                "द" to "2", "ध" to "2", "ड" to "2", "ढ" to "2",
                "ड़" to "2", "ढ़" to "2",
                // 3: त, थ, ट, ठ
                "त" to "3", "थ" to "3", "ट" to "3", "ठ" to "3",
                // 4: च, ज, र
                "च" to "4", "ज" to "4", "र" to "4",
                // 5: प, फ, फ़
                "प" to "5", "फ" to "5", "फ़" to "5",
                // 6: छ, झ
                "छ" to "6", "झ" to "6",
                // 7: स
                "स" to "7",
                // 8: ब, भ
                "ब" to "8", "भ" to "8",
                // 9: न, म, ण
                "न" to "9", "म" to "9", "ण" to "9"
            ),
            carrierConsonants = setOf("य", "व", "ह", "ल"),
            independentVowelMapping = mapOf(
                "उ" to "0", "ऊ" to "0",
                "ए" to "1", "ऐ" to "1",
                "ओ" to "2", "औ" to "2",
                "इ" to "3", "ई" to "3",
                "ऋ" to "4",
                "आ" to "8"
            ),
            matraMapping = mapOf(
                "ु" to "0", "ू" to "0",
                "े" to "1", "ै" to "1",
                "ो" to "2", "ौ" to "2",
                "ि" to "3", "ी" to "3",
                "ृ" to "4",
                "ा" to "8"
            ),
            diacriticMapping = mapOf(
                "ं" to "5", // Anusvara
                "ः" to "6"  // Visarga
            ),
            zeroDigitSigns = setOf(
                "ँ", // Chandrabindu: no digit in V2.3
                "अ", // Independent 'a': no digit
                "्", // Virama / Halant: no digit
                "़"  // Nukta: not standalone digit
            ),
            unsupportedSymbols = setOf(
                "ऑ", "ॉ", "ऍ", "ॅ", "ॆ", "ॊ", "ॐ", "।", "॥"
            )
        )
    }
}
