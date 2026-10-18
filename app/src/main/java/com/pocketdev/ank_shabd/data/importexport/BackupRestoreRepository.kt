package com.pocketdev.ank_shabd.data.importexport

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.JsonWriter
import android.util.JsonReader
import android.util.JsonToken
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.data.repository.DictionaryRepository
import com.pocketdev.ank_shabd.data.repository.NotesRepository
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.io.IOException

class BackupRestoreRepository(
    private val context: Context,
    private val dictionaryRepository: DictionaryRepository,
    private val notesRepository: NotesRepository
) {
    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)
    private val backupTables = listOf(
        "peg_words",
        "notebooks",
        "note_folders",
        "notes",
        "translation_records",
        "memory_stories",
        "flashcards",
        "review_records",
        "rulesets"
    )

    private companion object {
        const val CLIPBOARD_BACKUP_LIMIT_BYTES = 200_000
    }

    suspend fun generateValidationPreview(
        rawContent: String,
        format: String = "JSON"
    ): ImportValidationPreview = withContext(Dispatchers.IO) {
        val candidateWords = parseContentIntoRawRecords(rawContent, format)
        val validList = mutableListOf<PegWord>()
        val mismatches = mutableListOf<Pair<String, String>>()
        val unsupportedSymbols = mutableListOf<String>()
        var warningsCount = 0
        var duplicatesCount = 0

        val existingWords = dictionaryRepository.getAllPegs().associateBy { it.word }

        for (raw in candidateWords) {
            val enc = PhoneticMatraEncoder.encode(raw.word)
            if (enc.warnings.isNotEmpty()) {
                warningsCount += enc.warnings.size
                unsupportedSymbols.addAll(enc.warnings)
            }

            if (existingWords.containsKey(raw.word)) {
                duplicatesCount++
            }

            if (raw.expectedCode.isNotBlank() && raw.expectedCode != enc.code) {
                mismatches.add(raw.word to "${raw.expectedCode} (अपेक्षित) ≠ ${enc.code} (नियम अनुसार)")
            }

            val peg = PegWord(
                id = UUID.randomUUID().toString(),
                word = raw.word,
                normalizedWord = enc.normalizedWord,
                code = enc.code,
                ruleSetId = EncodingRuleSet.DEFAULT_RULESET_ID,
                category = raw.category.ifBlank { "सामान्य" },
                meaningHi = raw.meaningHi,
                meaningEn = raw.meaningEn,
                transliteration = raw.transliteration,
                qualityGrade = raw.qualityGrade.ifBlank { "Unverified" },
                verificationStatus = if (raw.expectedCode.isBlank() || raw.expectedCode == enc.code) "approved" else "unverified",
                verificationSource = "imported",
                isFavorite = false,
                tags = listOf("imported")
            )
            validList.add(peg)
        }

        ImportValidationPreview(
            totalRecords = candidateWords.size,
            validMatchesCount = validList.size - mismatches.size,
            codeMismatchesCount = mismatches.size,
            warningsCount = warningsCount,
            duplicateCount = duplicatesCount,
            sampleValid = validList.take(10),
            sampleMismatches = mismatches.take(10),
            unsupportedSymbols = unsupportedSymbols.distinct().take(10)
        )
    }

    suspend fun executeImport(
        rawContent: String,
        format: String = "JSON",
        duplicatePolicy: String = "SKIP" // "SKIP", "OVERWRITE", "KEEP_EXISTING"
    ): ImportReport = withContext(Dispatchers.IO) {
        val candidateWords = parseContentIntoRawRecords(rawContent, format)
        val db = dbHelper.writableDatabase
        var accepted = 0
        var rejected = 0
        var skipped = 0
        var updated = 0
        val errors = mutableListOf<String>()

        val existingWords = dictionaryRepository.getAllPegs().associateBy { it.word }

        db.beginTransaction()
        try {
            for (raw in candidateWords) {
                if (raw.word.isBlank()) {
                    rejected++
                    continue
                }

                val enc = PhoneticMatraEncoder.encode(raw.word)
                val calculatedCode = enc.code

                if (existingWords.containsKey(raw.word)) {
                    when (duplicatePolicy) {
                        "SKIP" -> {
                            skipped++
                            continue
                        }
                        "OVERWRITE" -> {
                            val existing = existingWords[raw.word]!!
                            val cv = ContentValues().apply {
                                put("code", calculatedCode)
                                put("normalized_word", enc.normalizedWord)
                                if (raw.meaningHi.isNotBlank()) put("meaning_hi", raw.meaningHi)
                                if (raw.meaningEn.isNotBlank()) put("meaning_en", raw.meaningEn)
                                put("quality_grade", raw.qualityGrade.ifBlank { existing.qualityGrade })
                                put("updated_at", System.currentTimeMillis())
                            }
                            db.update("peg_words", cv, "id = ?", arrayOf(existing.id))
                            updated++
                        }
                        else -> {
                            skipped++
                            continue
                        }
                    }
                } else {
                    val cv = ContentValues().apply {
                        put("id", UUID.randomUUID().toString())
                        put("word", raw.word)
                        put("normalized_word", enc.normalizedWord)
                        put("code", calculatedCode)
                        put("ruleset_id", EncodingRuleSet.DEFAULT_RULESET_ID)
                        put("category", raw.category.ifBlank { "सामान्य" })
                        put("meaning_hi", raw.meaningHi)
                        put("meaning_en", raw.meaningEn)
                        put("transliteration", raw.transliteration)
                        put("quality_grade", raw.qualityGrade.ifBlank { "Unverified" })
                        put("verification_status", if (raw.expectedCode.isBlank() || raw.expectedCode == calculatedCode) "approved" else "unverified")
                        put("verification_source", "imported")
                        put("is_favorite", 0)
                        put("tags", "[\"imported\"]")
                        put("created_at", System.currentTimeMillis())
                        put("updated_at", System.currentTimeMillis())
                    }
                    db.insert("peg_words", null, cv)
                    accepted++
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            errors.add("आयात त्रुटि: ${e.message}")
        } finally {
            db.endTransaction()
        }

        ImportReport(
            acceptedCount = accepted,
            rejectedCount = rejected,
            skippedDuplicatesCount = skipped,
            updatedCount = updated,
            errorDetails = errors
        )
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val output = ByteArrayOutputStream(64 * 1024)
        val limitedOutput = object : OutputStream() {
            override fun write(value: Int) {
                if (output.size() >= CLIPBOARD_BACKUP_LIMIT_BYTES) {
                    throw IOException("पूरा बैकअप clipboard के लिए बहुत बड़ा है। फ़ाइल निर्यात करें।")
                }
                output.write(value)
            }

            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                if (output.size() + length > CLIPBOARD_BACKUP_LIMIT_BYTES) {
                    throw IOException("पूरा बैकअप clipboard के लिए बहुत बड़ा है। फ़ाइल निर्यात करें।")
                }
                output.write(buffer, offset, length)
            }
        }
        exportFullBackupJson(limitedOutput)
        output.toString(Charsets.UTF_8.name())
    }

    suspend fun exportFullBackupJson(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val writer = JsonWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).apply { setIndent("  ") }
        writer.use {
            writer.beginObject()
            writer.name("schemaVersion").value(1)
            writer.name("appVersion").value("1.0")
            writer.name("ruleSetId").value(EncodingRuleSet.DEFAULT_RULESET_ID)
            writer.name("timestamp").value(System.currentTimeMillis())
            writer.name("tables").beginObject()
            val db = dbHelper.readableDatabase
            for (table in backupTables) {
                writer.name(table).beginArray()
                db.query(table, null, null, null, null, null, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        writer.beginObject()
                        for (columnIndex in 0 until cursor.columnCount) {
                            writer.name(cursor.getColumnName(columnIndex))
                            when (cursor.getType(columnIndex)) {
                                Cursor.FIELD_TYPE_NULL -> writer.nullValue()
                                Cursor.FIELD_TYPE_INTEGER -> writer.value(cursor.getLong(columnIndex))
                                Cursor.FIELD_TYPE_FLOAT -> writer.value(cursor.getDouble(columnIndex))
                                Cursor.FIELD_TYPE_BLOB -> writer.value(
                                    android.util.Base64.encodeToString(cursor.getBlob(columnIndex), android.util.Base64.NO_WRAP)
                                )
                                else -> writer.value(cursor.getString(columnIndex))
                            }
                        }
                        writer.endObject()
                    }
                }
                writer.endArray()
            }
            writer.endObject()
            writer.endObject()
        }
    }

    suspend fun exportDictionaryJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("ruleSetId", EncodingRuleSet.DEFAULT_RULESET_ID)
        val arr = JSONArray()
        for (w in dictionaryRepository.getAllPegs()) {
            val obj = JSONObject().apply {
                put("word", w.word)
                put("code", w.code)
                put("meaningHi", w.meaningHi)
                put("meaningEn", w.meaningEn)
                put("category", w.category)
                put("qualityGrade", w.qualityGrade)
                put("verificationStatus", w.verificationStatus)
            }
            arr.put(obj)
        }
        root.put("words", arr)
        root.toString(2)
    }

    suspend fun exportDictionaryCsv(): String = withContext(Dispatchers.IO) {
        val sb = java.lang.StringBuilder()
        sb.append("word,code,meaningHi,meaningEn,category,qualityGrade\n")
        for (w in dictionaryRepository.getAllPegs()) {
            sb.append("\"${w.word}\",\"${w.code}\",\"${w.meaningHi}\",\"${w.meaningEn}\",\"${w.category}\",\"${w.qualityGrade}\"\n")
        }
        sb.toString()
    }

    suspend fun validateBackupJson(jsonString: String): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val schemaVersion = root.optInt("schemaVersion", 1)
            val ruleSetId = root.optString("ruleSetId", EncodingRuleSet.DEFAULT_RULESET_ID)
            val tables = root.optJSONObject("tables")
            val words = tables?.optJSONArray("peg_words")?.length() ?: root.optJSONArray("words")?.length() ?: 0
            val notes = tables?.optJSONArray("notes")?.length() ?: root.optJSONArray("notes")?.length() ?: 0
            val flashcards = tables?.optJSONArray("flashcards")?.length() ?: root.optJSONArray("flashcards")?.length() ?: 0
            val stories = tables?.optJSONArray("memory_stories")?.length() ?: root.optJSONArray("stories")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                schemaVersion = schemaVersion,
                ruleSetId = ruleSetId,
                wordCount = words,
                noteCount = notes,
                flashcardCount = flashcards,
                storyCount = stories
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                schemaVersion = 0,
                ruleSetId = "",
                wordCount = 0,
                noteCount = 0,
                flashcardCount = 0,
                storyCount = 0,
                error = "अमान्य JSON संरचना: ${e.message}"
            )
        }
    }

    suspend fun restoreFromJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        val validation = validateBackupJson(jsonString)
        if (!validation.isValid) {
            return@withContext RestoreResult(false, 0, 0, 0, validation.error ?: "अमान्य बैकअप फाइल")
        }

        val db = dbHelper.writableDatabase
        var wordsRestored = 0
        var notesRestored = 0
        var flashcardsRestored = 0

        db.beginTransaction()
        try {
            val root = JSONObject(jsonString)
            val tables = root.optJSONObject("tables")

            val wordsArr = root.optJSONArray("words")
            if (tables == null && wordsArr != null) {
                for (i in 0 until wordsArr.length()) {
                    val obj = wordsArr.getJSONObject(i)
                    val word = obj.getString("word")
                    val enc = PhoneticMatraEncoder.encode(word)

                    val cv = ContentValues().apply {
                        put("id", obj.optString("id", UUID.randomUUID().toString()))
                        put("word", word)
                        put("normalized_word", enc.normalizedWord)
                        put("code", enc.code) // Re-verified code
                        put("ruleset_id", obj.optString("ruleSetId", EncodingRuleSet.DEFAULT_RULESET_ID))
                        put("category", obj.optString("category", "सामान्य"))
                        put("meaning_hi", obj.optString("meaningHi", ""))
                        put("meaning_en", obj.optString("meaningEn", ""))
                        put("transliteration", obj.optString("transliteration", ""))
                        put("quality_grade", obj.optString("qualityGrade", "A"))
                        put("verification_status", obj.optString("verificationStatus", "approved"))
                        put("verification_source", "restored")
                        put("is_favorite", if (obj.optBoolean("isFavorite", false)) 1 else 0)
                        put("created_at", System.currentTimeMillis())
                        put("updated_at", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict("peg_words", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    wordsRestored++
                }
            }

            val notesArr = root.optJSONArray("notes")
            if (tables == null && notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("id", obj.optString("id", UUID.randomUUID().toString()))
                        put("notebook_id", obj.optString("notebookId", "starter_notebook_1"))
                        put("title", obj.getString("title"))
                        put("content", obj.optString("content", ""))
                        put("template_type", obj.optString("templateType", "FREE_MARKDOWN"))
                        if (obj.isNull("targetNumber")) putNull("target_number")
                        else put("target_number", obj.optString("targetNumber"))
                        if (obj.isNull("targetWord")) putNull("target_word")
                        else put("target_word", obj.optString("targetWord"))
                        put("is_favorite", if (obj.optBoolean("isFavorite", false)) 1 else 0)
                        put("created_at", obj.optLong("createdAt", System.currentTimeMillis()))
                        put("updated_at", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    notesRestored++
                }
            }

            if (tables != null) {
                for (table in backupTables) {
                    val rows = tables.optJSONArray(table) ?: continue
                    val columns = db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                        buildSet {
                            while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                        }
                    }
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONObject(i)
                        val values = ContentValues()
                        val keys = row.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (key !in columns) continue
                            when (val value = row.opt(key)) {
                                null, JSONObject.NULL -> values.putNull(key)
                                is Boolean -> values.put(key, if (value) 1 else 0)
                                is Int -> values.put(key, value)
                                is Long -> values.put(key, value)
                                is Double -> values.put(key, value)
                                else -> values.put(key, value.toString())
                            }
                        }
                        val result = db.insertWithOnConflict(
                            table,
                            null,
                            values,
                            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                        )
                        if (result == -1L) throw IllegalStateException("बैकअप तालिका $table में पंक्ति $i सुरक्षित नहीं हुई।")
                        when (table) {
                            "peg_words" -> wordsRestored++
                            "notes" -> notesRestored++
                            "flashcards" -> flashcardsRestored++
                        }
                    }
                }
            }

            db.setTransactionSuccessful()
            RestoreResult(
                true,
                wordsRestored,
                notesRestored,
                flashcardsRestored,
                "डेटा सफलता पूर्वक पुनर्स्थापित किया गया ($wordsRestored शब्द, $notesRestored नोट्स, $flashcardsRestored फ्लैशकार्ड)"
            )
        } catch (e: Exception) {
            RestoreResult(false, 0, 0, 0, "पुनर्स्थापना विफल: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    suspend fun restoreFromJson(inputStream: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        var wordsRestored = 0
        var notesRestored = 0
        var flashcardsRestored = 0
        var restoredAnyTable = false
        db.beginTransaction()
        try {
            JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "tables" -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val table = reader.nextName()
                                if (table !in backupTables) {
                                    reader.skipValue()
                                    continue
                                }
                                val columns = db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                                    buildSet {
                                        while (cursor.moveToNext()) {
                                            add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                                        }
                                    }
                                }
                                reader.beginArray()
                                var rowIndex = 0
                                while (reader.hasNext()) {
                                    val row = readJsonValue(reader) as? JSONObject
                                        ?: throw IllegalArgumentException("तालिका $table में अमान्य पंक्ति $rowIndex।")
                                    val values = ContentValues()
                                    val keys = row.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        if (key !in columns) continue
                                        when (val value = row.opt(key)) {
                                            null, JSONObject.NULL -> values.putNull(key)
                                            is Boolean -> values.put(key, if (value) 1 else 0)
                                            is Int -> values.put(key, value)
                                            is Long -> values.put(key, value)
                                            is Double -> values.put(key, value)
                                            else -> values.put(key, value.toString())
                                        }
                                    }
                                    val result = db.insertWithOnConflict(
                                        table,
                                        null,
                                        values,
                                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                                    )
                                    if (result == -1L) throw IllegalStateException("बैकअप तालिका $table में पंक्ति $rowIndex सुरक्षित नहीं हुई।")
                                    when (table) {
                                        "peg_words" -> wordsRestored++
                                        "notes" -> notesRestored++
                                        "flashcards" -> flashcardsRestored++
                                    }
                                    rowIndex++
                                }
                                reader.endArray()
                                restoredAnyTable = true
                            }
                            reader.endObject()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            if (!restoredAnyTable) throw IllegalArgumentException("बैकअप में पुनर्स्थापित करने योग्य डेटा नहीं मिला।")
            db.setTransactionSuccessful()
            RestoreResult(
                true,
                wordsRestored,
                notesRestored,
                flashcardsRestored,
                "डेटा सफलता पूर्वक पुनर्स्थापित किया गया ($wordsRestored शब्द, $notesRestored नोट्स, $flashcardsRestored फ्लैशकार्ड)"
            )
        } catch (e: Exception) {
            RestoreResult(false, 0, 0, 0, "पुनर्स्थापना विफल: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } finally {
            db.endTransaction()
            inputStream.close()
        }
    }

    private fun readJsonValue(reader: JsonReader): Any? = when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            val objectValue = JSONObject()
            reader.beginObject()
            while (reader.hasNext()) objectValue.put(reader.nextName(), readJsonValue(reader) ?: JSONObject.NULL)
            reader.endObject()
            objectValue
        }
        JsonToken.BEGIN_ARRAY -> {
            val arrayValue = JSONArray()
            reader.beginArray()
            while (reader.hasNext()) arrayValue.put(readJsonValue(reader) ?: JSONObject.NULL)
            reader.endArray()
            arrayValue
        }
        JsonToken.STRING -> reader.nextString()
        JsonToken.NUMBER -> reader.nextString().let { number ->
            if (number.contains('.') || number.contains('e', true)) number.toDouble() else number.toLongOrNull() ?: number
        }
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> {
            reader.nextNull()
            JSONObject.NULL
        }
        else -> throw IllegalArgumentException("बैकअप JSON में अमान्य मान मिला।")
    }

    private fun parseContentIntoRawRecords(content: String, format: String): List<RawImportItem> {
        val list = mutableListOf<RawImportItem>()
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return list

        if (format.equals("JSON", ignoreCase = true) || trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    val arr = root.optJSONArray("words") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            RawImportItem(
                                word = obj.getString("word"),
                                expectedCode = obj.optString("code", ""),
                                meaningHi = obj.optString("meaningHi", ""),
                                meaningEn = obj.optString("meaningEn", ""),
                                category = obj.optString("category", ""),
                                transliteration = obj.optString("transliteration", ""),
                                qualityGrade = obj.optString("qualityGrade", "")
                            )
                        )
                    }
                } else {
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            RawImportItem(
                                word = obj.getString("word"),
                                expectedCode = obj.optString("code", ""),
                                meaningHi = obj.optString("meaningHi", ""),
                                meaningEn = obj.optString("meaningEn", ""),
                                category = obj.optString("category", ""),
                                transliteration = obj.optString("transliteration", ""),
                                qualityGrade = obj.optString("qualityGrade", "")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Try fallback line parsing if JSON fails
            }
        }

        // CSV or Plain Text list fallback
        if (list.isEmpty()) {
            val lines = trimmed.lines()
            for (line in lines) {
                val l = line.trim()
                if (l.isBlank() || l.startsWith("#")) continue
                val parts = l.split(",")
                if (parts.size >= 2) {
                    list.add(
                        RawImportItem(
                            word = parts[0].trim(),
                            expectedCode = parts[1].trim(),
                            meaningHi = parts.getOrNull(2)?.trim() ?: "",
                            meaningEn = parts.getOrNull(3)?.trim() ?: "",
                            category = parts.getOrNull(4)?.trim() ?: "",
                            qualityGrade = parts.getOrNull(5)?.trim() ?: "A"
                        )
                    )
                } else {
                    // Just word per line
                    list.add(RawImportItem(word = l, expectedCode = ""))
                }
            }
        }

        return list
    }

    private data class RawImportItem(
        val word: String,
        val expectedCode: String,
        val meaningHi: String = "",
        val meaningEn: String = "",
        val category: String = "",
        val transliteration: String = "",
        val qualityGrade: String = ""
    )
}
