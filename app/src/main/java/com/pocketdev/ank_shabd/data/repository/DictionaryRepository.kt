package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class DictionaryRepository(context: Context) {

    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)

    suspend fun getPegsByCode(code: String): List<PegWord> = withContext(Dispatchers.IO) {
        getPegsByCodeSync(code)
    }

    fun getPegsByCodeSync(code: String): List<PegWord> {
        val list = mutableListOf<PegWord>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "peg_words",
            null,
            "code = ?",
            arrayOf(code),
            null,
            null,
            "CASE verification_status WHEN 'approved' THEN 0 WHEN 'unverified' THEN 1 ELSE 2 END ASC, quality_grade ASC, word ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPegWord(it))
            }
        }
        return list
    }

    suspend fun getPegByWord(word: String): PegWord? = withContext(Dispatchers.IO) {
        val normalized = PhoneticMatraEncoder.encode(word).normalizedWord
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "peg_words",
            null,
            "word = ? OR normalized_word = ?",
            arrayOf(word.trim(), normalized),
            null,
            null,
            null,
            "1"
        )
        cursor.use {
            if (it.moveToNext()) {
                cursorToPegWord(it)
            } else {
                null
            }
        }
    }

    suspend fun getAllPegs(
        query: String = "",
        gradeFilter: String = "ALL",
        categoryFilter: String = "ALL",
        favoritesOnly: Boolean = false
    ): List<PegWord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PegWord>()
        val db = dbHelper.readableDatabase

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (query.isNotBlank()) {
            val q = "%${query.trim()}%"
            selectionParts.add("(word LIKE ? OR normalized_word LIKE ? OR code LIKE ? OR meaning_hi LIKE ? OR meaning_en LIKE ? OR transliteration LIKE ?)")
            repeat(6) { selectionArgs.add(q) }
        }

        if (gradeFilter != "ALL") {
            selectionParts.add("quality_grade = ?")
            selectionArgs.add(gradeFilter)
        }

        if (categoryFilter != "ALL") {
            selectionParts.add("category = ?")
            selectionArgs.add(categoryFilter)
        }

        if (favoritesOnly) {
            selectionParts.add("is_favorite = 1")
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor = db.query(
            "peg_words",
            null,
            selection,
            args,
            null,
            null,
            "code ASC, quality_grade ASC, word ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPegWord(it))
            }
        }
        list
    }

    /**
     * Inserts or updates a peg word ONLY if the deterministic V2.3 encoder confirms a valid encoding.
     * Prevents duplicate normalized_word + code entries, and protects existing approved pegs from
     * silent overwrite unless [allowOverwriteApproved] is true.
     */
    suspend fun insertOrUpdatePeg(
        peg: PegWord,
        expectedCode: String? = null,
        allowOverwriteApproved: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        // Always recalculate code using deterministic encoder to ensure data integrity
        val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(peg.word)
        require(enc.isValid) {
            "अमान्य शब्द '${peg.word}': ${enc.warnings.joinToString("; ")}"
        }
        val verifiedCode = enc.code
        if (expectedCode != null && expectedCode.isNotBlank()) {
            require(verifiedCode == expectedCode) {
                "कोड मेल नहीं खाता: '${peg.word}' का V2.3 कोड '$verifiedCode' है, न कि '$expectedCode'।"
            }
        }

        // Check if a peg with the same normalized_word and code already exists
        val existingSameWord = getPegByWord(enc.normalizedWord)
        if (existingSameWord != null && existingSameWord.verificationStatus == "approved" && !allowOverwriteApproved) {
            return@withContext -1L
        }

        val targetId = existingSameWord?.id ?: peg.id.ifBlank { UUID.randomUUID().toString() }
        val breakdown = peg.encodingBreakdown?.takeIf { it.isNotBlank() } ?: enc.formattedTraceSummary()
        val story = peg.mnemonicStory?.takeIf { it.isNotBlank() }
            ?: SeedDictionary.buildVividMnemonicStory(
                word = enc.normalizedWord,
                code = verifiedCode,
                meaningHi = peg.meaningHi,
                visualAssociation = peg.visualAssociation
            )

        val cv = ContentValues().apply {
            put("id", targetId)
            put("word", peg.word.trim())
            put("normalized_word", enc.normalizedWord)
            put("code", verifiedCode)
            put("ruleset_id", peg.ruleSetId.ifBlank { EncodingRuleSet.DEFAULT_RULESET_ID })
            put("category", peg.category.ifBlank { "सामान्य" })
            put("meaning_hi", peg.meaningHi)
            put("meaning_en", peg.meaningEn)
            put("transliteration", peg.transliteration)
            put("quality_grade", peg.qualityGrade)
            put("verification_status", peg.verificationStatus)
            put("verification_source", peg.verificationSource)
            put("rejection_reason", peg.rejectionReason)
            put("visual_association", peg.visualAssociation)
            put("mnemonic_story", story)
            put("encoding_breakdown", breakdown)
            put("encoder_version", peg.encoderVersion.ifBlank { PhoneticMatraEncoder.ENCODER_VERSION })
            put("dictionary_evidence", peg.dictionaryEvidence ?: "")
            put("commonness", peg.commonness ?: "medium")
            put("confidence", peg.confidence ?: 1.0f)
            put("example_sentence_hi", peg.exampleSentenceHi)
            put("example_sentence_en", peg.exampleSentenceEn)
            put("is_favorite", if (peg.isFavorite) 1 else 0)
            put("tags", JSONArray(peg.tags).toString())
            put("created_at", existingSameWord?.createdAt ?: peg.createdAt)
            put("updated_at", System.currentTimeMillis())
        }

        db.insertWithOnConflict("peg_words", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun savePeg(
        peg: PegWord,
        expectedCode: String? = null,
        allowOverwriteApproved: Boolean = true
    ): Long = insertOrUpdatePeg(peg, expectedCode, allowOverwriteApproved)

    /**
     * Checks if there is already an approved peg for [code] with a different word,
     * or if the exact same word is already approved.
     */
    suspend fun getApprovedPegsForCode(code: String): List<PegWord> = withContext(Dispatchers.IO) {
        getPegsByCodeSync(code).filter { it.verificationStatus == "approved" && (it.qualityGrade == "A" || it.qualityGrade == "B") }
    }

    /**
     * Identifies words in the local database that need revalidation because the mapping ruleset,
     * encoder version, or recalculated code has changed.
     */
    suspend fun getWordsNeedingRevalidation(
        expectedEncoderVersion: String = PhoneticMatraEncoder.ENCODER_VERSION,
        ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3
    ): List<PegWord> = withContext(Dispatchers.IO) {
        getAllPegs().filter { peg ->
            val recalculated = PhoneticMatraEncoder.encodeSingleWordCandidate(peg.word, ruleSet)
            !recalculated.isValid ||
                recalculated.code != peg.code ||
                peg.encoderVersion != expectedEncoderVersion ||
                peg.ruleSetId != ruleSet.id
        }
    }

    suspend fun getRecentPegs(limit: Int = 10): List<PegWord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PegWord>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("peg_words", null, null, null, null, null, "updated_at DESC", limit.toString())
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPegWord(it))
            }
        }
        list
    }

    suspend fun updateQualityGrade(word: String, grade: String): Boolean = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("quality_grade", grade)
            put("verification_status", "approved")
            put("updated_at", System.currentTimeMillis())
        }
        val rows = dbHelper.writableDatabase.update("peg_words", cv, "word = ?", arrayOf(word))
        rows > 0
    }

    suspend fun updateDictionaryFolder(id: String, folder: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("category", folder.trim())
            put("updated_at", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.update("peg_words", values, "id = ?", arrayOf(id)) > 0
    }

    suspend fun deletePeg(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val rows = db.delete("peg_words", "id = ? OR word = ?", arrayOf(id, id))
        rows > 0
    }

    suspend fun toggleFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.execSQL("UPDATE peg_words SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END, updated_at = ? WHERE id = ? OR word = ?", arrayOf(System.currentTimeMillis(), id, id))
        true
    }

    suspend fun updateVerification(id: String, status: String, grade: String, reason: String? = null): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("verification_status", status)
            put("quality_grade", grade)
            if (reason != null) put("rejection_reason", reason)
            put("updated_at", System.currentTimeMillis())
        }
        val rows = db.update("peg_words", cv, "id = ? OR word = ?", arrayOf(id, id))
        rows > 0
    }

    suspend fun getCoverageStats(targetRange: String = "00–99"): CoverageStats = withContext(Dispatchers.IO) {
        val allWords = getAllPegs()
        val approvedWords = allWords.filter { it.verificationStatus == "approved" }

        val totalUniqueCodes = approvedWords.map { it.code }.distinct().size
        val totalApprovedA = approvedWords.count { it.qualityGrade == "A" }
        val totalB = approvedWords.count { it.qualityGrade == "B" }
        val totalUnverified = allWords.count { it.verificationStatus == "unverified" || it.qualityGrade == "Unverified" }

        // Determine range slot coverage
        var totalTargetSlots = 100
        var filledApprovedSlots = 0
        var emptySlotCount = 0

        if (targetRange == "00–99") {
            totalTargetSlots = 100
            for (i in 0..99) {
                val code = String.format("%02d", i)
                val hasApproved = approvedWords.any { it.code == code && (it.qualityGrade == "A" || it.qualityGrade == "B") }
                if (hasApproved) {
                    filledApprovedSlots++
                } else {
                    emptySlotCount++
                }
            }
        } else if (targetRange == "000–999") {
            totalTargetSlots = 1000
            for (i in 0..999) {
                val code = String.format("%03d", i)
                val hasApproved = approvedWords.any { it.code == code }
                if (hasApproved) filledApprovedSlots++ else emptySlotCount++
            }
        } else {
            totalTargetSlots = totalUniqueCodes + 1
            filledApprovedSlots = totalUniqueCodes
            emptySlotCount = 0
        }

        val percentage = (filledApprovedSlots.toFloat() / totalTargetSlots.toFloat()) * 100f

        // Count duplicate code groups (codes having more than 1 peg word)
        val duplicateGroups = allWords.groupBy { it.code }.count { it.value.size > 1 }

        CoverageStats(
            totalUniqueCodes = totalUniqueCodes,
            totalApprovedA = totalApprovedA,
            totalB = totalB,
            totalUnverified = totalUnverified,
            totalEmptySlots = emptySlotCount,
            duplicateGroups = duplicateGroups,
            targetRange = targetRange,
            coveragePercentage = percentage
        )
    }

    suspend fun searchUniversal(query: String): UniversalSearchResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext UniversalSearchResult(query, emptyList(), emptyList(), emptyList(), emptyList())
        }

        val matchingPegs = getAllPegs(query = trimmed)

        // Notes search
        val matchingNotes = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val nCursor = db.query(
            "notes",
            null,
            "title LIKE ? OR content LIKE ? OR target_number LIKE ? OR target_word LIKE ? OR tags LIKE ?",
            arrayOf("%$trimmed%", "%$trimmed%", "%$trimmed%", "%$trimmed%", "%$trimmed%"),
            null,
            null,
            "updated_at DESC",
            "20"
        )
        nCursor.use {
            while (it.moveToNext()) {
                matchingNotes.add(
                    Note(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        notebookId = it.getString(it.getColumnIndexOrThrow("notebook_id")),
                        folderId = it.getString(it.getColumnIndexOrThrow("folder_id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        templateType = it.getString(it.getColumnIndexOrThrow("template_type")),
                        targetNumber = it.getString(it.getColumnIndexOrThrow("target_number")),
                        targetWord = it.getString(it.getColumnIndexOrThrow("target_word")),
                        tags = parseJsonArray(it.getString(it.getColumnIndexOrThrow("tags"))),
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                    )
                )
            }
        }

        // Translations search
        val matchingTranslations = mutableListOf<TranslationRecord>()
        val tCursor = db.query(
            "translation_records",
            null,
            "source_text LIKE ? OR translated_text LIKE ? OR meaning_hi LIKE ? OR meaning_en LIKE ?",
            arrayOf("%$trimmed%", "%$trimmed%", "%$trimmed%", "%$trimmed%"),
            null,
            null,
            "created_at DESC",
            "10"
        )
        tCursor.use {
            while (it.moveToNext()) {
                matchingTranslations.add(
                    TranslationRecord(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sourceText = it.getString(it.getColumnIndexOrThrow("source_text")),
                        sourceLang = it.getString(it.getColumnIndexOrThrow("source_lang")),
                        targetLang = it.getString(it.getColumnIndexOrThrow("target_lang")),
                        translatedText = it.getString(it.getColumnIndexOrThrow("translated_text")),
                        transliteration = it.getString(it.getColumnIndexOrThrow("transliteration")),
                        meaningHi = it.getString(it.getColumnIndexOrThrow("meaning_hi")),
                        meaningEn = it.getString(it.getColumnIndexOrThrow("meaning_en")),
                        partOfSpeech = it.getString(it.getColumnIndexOrThrow("part_of_speech")),
                        provider = it.getString(it.getColumnIndexOrThrow("provider")),
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }

        // Stories search
        val matchingStories = mutableListOf<MemoryStory>()
        val sCursor = db.query(
            "memory_stories",
            null,
            "story_title LIKE ? OR story_hi LIKE ? OR number_sequence LIKE ? OR peg_words LIKE ?",
            arrayOf("%$trimmed%", "%$trimmed%", "%$trimmed%", "%$trimmed%"),
            null,
            null,
            "created_at DESC",
            "10"
        )
        sCursor.use {
            while (it.moveToNext()) {
                matchingStories.add(
                    MemoryStory(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        numberSequence = it.getString(it.getColumnIndexOrThrow("number_sequence")),
                        codeSegments = parseJsonArray(it.getString(it.getColumnIndexOrThrow("code_segments"))),
                        pegWords = parseJsonArray(it.getString(it.getColumnIndexOrThrow("peg_words"))),
                        storyTitle = it.getString(it.getColumnIndexOrThrow("story_title")),
                        storyHi = it.getString(it.getColumnIndexOrThrow("story_hi")),
                        storyEn = it.getString(it.getColumnIndexOrThrow("story_en")),
                        visualScene = it.getString(it.getColumnIndexOrThrow("visual_scene")),
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        noteId = it.getString(it.getColumnIndexOrThrow("note_id")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }

        UniversalSearchResult(
            query = trimmed,
            matchingPegs = matchingPegs,
            matchingNotes = matchingNotes,
            matchingTranslations = matchingTranslations,
            matchingStories = matchingStories
        )
    }

    private fun cursorToPegWord(cursor: android.database.Cursor): PegWord {
        val word = cursor.getString(cursor.getColumnIndexOrThrow("word"))
        val code = cursor.getString(cursor.getColumnIndexOrThrow("code"))
        val meaningHi = cursor.getString(cursor.getColumnIndexOrThrow("meaning_hi"))
        val visualAssoc = cursor.getString(cursor.getColumnIndexOrThrow("visual_association"))
        val mnemonicColIdx = cursor.getColumnIndex("mnemonic_story")
        val breakdownColIdx = cursor.getColumnIndex("encoding_breakdown")
        val versionColIdx = cursor.getColumnIndex("encoder_version")
        val evidenceColIdx = cursor.getColumnIndex("dictionary_evidence")
        val commonnessColIdx = cursor.getColumnIndex("commonness")
        val confidenceColIdx = cursor.getColumnIndex("confidence")

        val rawStory = if (mnemonicColIdx >= 0) cursor.getString(mnemonicColIdx) else null
        val rawBreakdown = if (breakdownColIdx >= 0) cursor.getString(breakdownColIdx) else null
        val rawVersion = if (versionColIdx >= 0) cursor.getString(versionColIdx) else "2.3"
        val rawEvidence = if (evidenceColIdx >= 0) cursor.getString(evidenceColIdx) else null
        val rawCommonness = if (commonnessColIdx >= 0) cursor.getString(commonnessColIdx) else "medium"
        val rawConfidence = if (confidenceColIdx >= 0) cursor.getFloat(confidenceColIdx) else 1.0f

        return PegWord(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            word = word,
            normalizedWord = cursor.getString(cursor.getColumnIndexOrThrow("normalized_word")),
            code = code,
            ruleSetId = cursor.getString(cursor.getColumnIndexOrThrow("ruleset_id")),
            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
            meaningHi = meaningHi,
            meaningEn = cursor.getString(cursor.getColumnIndexOrThrow("meaning_en")),
            transliteration = cursor.getString(cursor.getColumnIndexOrThrow("transliteration")),
            qualityGrade = cursor.getString(cursor.getColumnIndexOrThrow("quality_grade")),
            verificationStatus = cursor.getString(cursor.getColumnIndexOrThrow("verification_status")),
            verificationSource = cursor.getString(cursor.getColumnIndexOrThrow("verification_source")),
            rejectionReason = cursor.getString(cursor.getColumnIndexOrThrow("rejection_reason")),
            visualAssociation = visualAssoc,
            exampleSentenceHi = cursor.getString(cursor.getColumnIndexOrThrow("example_sentence_hi")),
            exampleSentenceEn = cursor.getString(cursor.getColumnIndexOrThrow("example_sentence_en")),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
            tags = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("tags"))),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            mnemonicStory = rawStory?.takeIf { it.isNotBlank() }
                ?: SeedDictionary.buildVividMnemonicStory(word, code, meaningHi, visualAssoc),
            encodingBreakdown = rawBreakdown?.takeIf { it.isNotBlank() }
                ?: PhoneticMatraEncoder.encode(word).formattedTraceSummary(),
            encoderVersion = rawVersion?.ifBlank { "2.3" } ?: "2.3",
            dictionaryEvidence = rawEvidence?.takeIf { it.isNotBlank() },
            commonness = rawCommonness?.takeIf { it.isNotBlank() },
            confidence = rawConfidence
        )
    }

    private fun parseJsonArray(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
