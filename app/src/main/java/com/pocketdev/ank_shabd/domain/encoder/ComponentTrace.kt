package com.pocketdev.ank_shabd.domain.encoder

enum class ComponentType {
    CONSONANT,
    CARRIER_CONSONANT,
    INDEPENDENT_VOWEL,
    MATRA,
    DIACRITIC,
    VIRAMA,
    NUKTA,
    UNSUPPORTED,
    SPACE_OR_PUNCTUATION
}

data class ComponentTrace(
    val character: String,
    val componentType: ComponentType,
    val digitEmitted: String?,
    val ruleApplied: String,
    val position: Int,
    val isWarning: Boolean = false,
    val warningMessage: String? = null
) {
    val token: String get() = character
    val explanation: String get() = digitEmitted ?: ruleApplied
}
