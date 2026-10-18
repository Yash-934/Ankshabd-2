package com.pocketdev.ank_shabd.domain.models

data class UniversalSearchResult(
    val query: String,
    val matchingPegs: List<PegWord>,
    val matchingNotes: List<Note>,
    val matchingTranslations: List<TranslationRecord>,
    val matchingStories: List<MemoryStory>
) {
    val totalCount: Int
        get() = matchingPegs.size + matchingNotes.size + matchingTranslations.size + matchingStories.size
}

data class RevisionStats(
    val totalCards: Int,
    val dueToday: Int,
    val learnedCount: Int,
    val accuracyPercentage: Float,
    val currentStreak: Int,
    val difficultWords: List<String>
) {
    val accuracyRate: Float get() = accuracyPercentage
}

data class ImportValidationPreview(
    val totalRecords: Int,
    val validMatchesCount: Int,
    val codeMismatchesCount: Int,
    val warningsCount: Int,
    val duplicateCount: Int,
    val sampleValid: List<PegWord>,
    val sampleMismatches: List<Pair<String, String>>, // (word, recalculatedCode vs expectedCode)
    val unsupportedSymbols: List<String>
)

data class ImportReport(
    val acceptedCount: Int,
    val rejectedCount: Int,
    val skippedDuplicatesCount: Int,
    val updatedCount: Int,
    val errorDetails: List<String>
) {
    val skippedCount: Int get() = skippedDuplicatesCount
}

data class BackupValidationResult(
    val isValid: Boolean,
    val schemaVersion: Int,
    val ruleSetId: String,
    val wordCount: Int,
    val noteCount: Int,
    val flashcardCount: Int,
    val storyCount: Int,
    val error: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val wordsRestored: Int,
    val notesRestored: Int,
    val flashcardsRestored: Int,
    val message: String
) {
    val isSuccess: Boolean get() = success
    val errorMessage: String get() = message
}

data class RuleSetDiff(
    val ruleSet1Id: String,
    val ruleSet2Id: String,
    val consonantChanges: List<String>,
    val carrierChanges: List<String>,
    val vowelChanges: List<String>,
    val matraChanges: List<String>
)
