package com.pocketdev.ank_shabd.domain.models

data class PegWord(
    val id: String,
    val word: String,
    val normalizedWord: String,
    val code: String,
    val ruleSetId: String,
    val category: String,
    val meaningHi: String,
    val meaningEn: String,
    val transliteration: String,
    val qualityGrade: String, // "A", "B", "C", "Unverified"
    val verificationStatus: String, // "approved", "unverified", "rejected"
    val verificationSource: String, // "v2_3_seed", "user_verified", "user_added", "gemini_generated", "ai_candidate", "imported"
    val rejectionReason: String? = null,
    val visualAssociation: String? = null,
    val exampleSentenceHi: String? = null,
    val exampleSentenceEn: String? = null,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val mnemonicStory: String? = null,
    val encodingBreakdown: String? = null,
    val encoderVersion: String = "2.3",
    val dictionaryEvidence: String? = null,
    val commonness: String? = null,
    val confidence: Float? = null
) {
    val visualScene: String
        get() = mnemonicStory?.takeIf { it.isNotBlank() }
            ?: visualAssociation?.takeIf { it.isNotBlank() }
            ?: ""
}

data class Notebook(
    val id: String,
    val name: String,
    val description: String,
    val colorHex: String = "#1E3A8A",
    val icon: String = "book",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class NoteFolder(
    val id: String,
    val notebookId: String,
    val name: String,
    val parentFolderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Note(
    val id: String,
    val notebookId: String,
    val folderId: String? = null,
    val title: String,
    val content: String,
    val templateType: String = "FREE_MARKDOWN", // "PEG_WORD", "VOCABULARY", "NUMBER_MEMORY", "FREE_MARKDOWN"
    val targetNumber: String? = null,
    val targetWord: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TranslationRecord(
    val id: String,
    val sourceText: String,
    val sourceLang: String = "hi",
    val targetLang: String = "en",
    val translatedText: String,
    val transliteration: String = "",
    val meaningHi: String = "",
    val meaningEn: String = "",
    val partOfSpeech: String = "",
    val exampleSentences: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val provider: String = "Local Rule Engine",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class MemoryStory(
    val id: String,
    val numberSequence: String,
    val codeSegments: List<String>,
    val pegWords: List<String>,
    val storyTitle: String,
    val storyHi: String,
    val storyEn: String? = null,
    val visualScene: String? = null,
    val isFavorite: Boolean = false,
    val noteId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Flashcard(
    val id: String,
    val cardType: String, // "NUMBER_TO_WORD", "WORD_TO_NUMBER", "WORD_TO_ENGLISH", "ENGLISH_TO_WORD", "TRACE_TO_CODE", "CHOOSE_VALID_PEG"
    val prompt: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val explanation: String = "",
    val wordId: String? = null,
    val code: String? = null,
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewedDate: Long? = null,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val streak: Int = 0
) {
    val frontPrompt: String get() = prompt
    val backAnswer: String get() = answer
    val vividHook: String get() = explanation
}

data class ReviewRecord(
    val id: String,
    val cardId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int, // 0 = Again, 1 = Hard, 2 = Good, 3 = Easy
    val timeSpentMs: Long = 0L
)

data class CoverageStats(
    val totalUniqueCodes: Int,
    val totalApprovedA: Int,
    val totalB: Int,
    val totalUnverified: Int,
    val totalEmptySlots: Int,
    val duplicateGroups: Int,
    val targetRange: String,
    val coveragePercentage: Float
) {
    val totalWords: Int get() = totalApprovedA + totalB + totalUnverified
    val coveredCodesCount: Int get() = totalUniqueCodes
    val gradeACount: Int get() = totalApprovedA
    val gradeBCount: Int get() = totalB
}
