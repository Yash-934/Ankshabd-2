package com.pocketdev.ank_shabd.domain.encoder

data class EncodingResult(
    val originalWord: String,
    val normalizedWord: String,
    val code: String,
    val traces: List<ComponentTrace>,
    val isValid: Boolean,
    val warnings: List<String>,
    val ruleSetId: String
) {
    fun formattedTraceSummary(): String {
        if (traces.isEmpty()) return ""
        val parts = traces.mapNotNull { trace ->
            when {
                trace.componentType == ComponentType.CARRIER_CONSONANT -> "${trace.character}(वाहक)"
                trace.digitEmitted != null -> "${trace.character}(${trace.digitEmitted})"
                trace.character == "ँ" -> "चंद्रबिंदु(∅)"
                trace.componentType == ComponentType.VIRAMA -> "हलंत(∅)"
                trace.character == "अ" -> "अ(∅)"
                trace.isWarning -> "${trace.character}(चेतावनी)"
                else -> null
            }
        }
        return if (parts.isNotEmpty()) "${parts.joinToString(" + ")} = $code" else code
    }

    fun visualSequence(): String {
        return traces.joinToString(" | ") { trace ->
            val effect = trace.digitEmitted ?: when (trace.componentType) {
                ComponentType.CARRIER_CONSONANT -> "वाहक"
                ComponentType.VIRAMA -> "हलंत"
                ComponentType.UNSUPPORTED -> "असमर्थित"
                else -> "∅"
            }
            "${trace.character} → $effect"
        }
    }
}
