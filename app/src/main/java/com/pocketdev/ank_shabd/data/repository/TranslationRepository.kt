package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.ai.AiProvider
import com.pocketdev.ank_shabd.ai.GeminiAiProvider
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.generator.NumberToWordGenerator
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import com.pocketdev.ank_shabd.domain.models.TranslationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class TranslationRepository(private val appContext: Context) {

    private val dbHelper = AnkShabdDatabaseHelper.getInstance(appContext)

    suspend fun translate(
        text: String,
        sourceLang: String = "hi",
        targetLang: String = "en",
        aiProvider: AiProvider? = null
    ): TranslationRecord = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (sourceLang == "number" || trimmed.any { it.isDigit() || it in '०'..'९' }) {
            val tokens = extractNumberTokens(trimmed)
            require(tokens.isNotEmpty()) { "कृपया मान्य संख्या (अंक) दर्ज करें।" }

            val dictRepo = DictionaryRepository(appContext)
            val generator = NumberToWordGenerator(
                dictionaryRepository = dictRepo,
                aiProvider = aiProvider as? GeminiAiProvider
            )

            val primaryLines = mutableListOf<String>()
            val detailLines = mutableListOf<String>()
            val attemptStepLogs = mutableListOf<String>()
            val allFoundWords = mutableListOf<String>()

            for (code in tokens) {
                val genResult = generator.generateForNumber(
                    rawNumber = code,
                    invokeAi = true,
                    ruleSet = EncodingRuleSet.V2_3
                )

                // Record every progressive attempt step (1 word -> 2 words -> 3 words...)
                genResult.progressiveAttempts.forEach { step ->
                    attemptStepLogs.add(step.summaryHi)
                }

                if (genResult.bestCandidate != null) {
                    // Succeeded in 1 single word!
                    val best = genResult.bestCandidate
                    val otherWords = genResult.allCandidates.drop(1).map { it.hindiWord }
                    val suffix = if (otherWords.isNotEmpty()) " (अन्य विकल्प: ${otherWords.joinToString(", ")})" else ""
                    primaryLines.add("$code → ${best.hindiWord} (1 शब्द)$suffix")
                    genResult.allCandidates.forEach { cand ->
                        if (!allFoundWords.contains(cand.hindiWord)) {
                            allFoundWords.add(cand.hindiWord)
                        }
                        detailLines.add("• ${cand.hindiWord} ($code) — अर्थ: ${cand.meaningHi} | कोडिंग: [${cand.characterByCharacterEncoding}]")
                    }
                } else if (genResult.segmentedBreakdown != null) {
                    // Succeeded in k words (2 words, 3 words, etc.) when 1 word was not found!
                    val seg = genResult.segmentedBreakdown
                    primaryLines.add("$code → ${seg.combinedWordChain} (${seg.wordCountUsed} शब्द)")
                    detailLines.add("• ${seg.whySegmentedExplanationHi}")
                    seg.segments.forEach { item ->
                        if (item.pegWord != null && !allFoundWords.contains(item.word)) {
                            allFoundWords.add(item.word)
                        }
                        detailLines.add("  - '${item.word}' (अंक ${item.segmentCode}): ${item.meaningHi} | कोडिंग: [${item.traceSummary}]")
                    }
                    if (seg.combinedVividStoryHi.isNotBlank()) {
                        detailLines.add("🎬 संयुक्त दृश्य कहानी: ${seg.combinedVividStoryHi}")
                    }
                }
            }

            val resultText = primaryLines.joinToString("\n")
            val record = TranslationRecord(
                id = UUID.randomUUID().toString(),
                sourceText = tokens.joinToString(", "),
                sourceLang = "number",
                targetLang = "hi",
                translatedText = resultText,
                meaningHi = detailLines.joinToString("\n"),
                meaningEn = "",
                partOfSpeech = "V2.3 संख्या → सार्थक हिंदी शब्द",
                exampleSentences = attemptStepLogs,
                synonyms = allFoundWords,
                provider = if (aiProvider?.isConfigured == true) "${aiProvider.providerName} + V2.3 एनकोडर" else "V2.3 प्रमाणित शब्दकोश",
                createdAt = System.currentTimeMillis()
            )
            saveTranslation(record)
            return@withContext record
        }

        if (aiProvider?.isConfigured == true) {
            val translated = aiProvider.translateText(trimmed, sourceLang, targetLang)
            val record = TranslationRecord(
                id = UUID.randomUUID().toString(),
                sourceText = trimmed,
                sourceLang = sourceLang,
                targetLang = targetLang,
                translatedText = translated,
                transliteration = if (sourceLang == "hi") generateLatinTransliteration(trimmed) else "",
                meaningHi = if (targetLang == "hi") translated else "",
                meaningEn = if (targetLang == "en") translated else "",
                provider = aiProvider.providerName,
                createdAt = System.currentTimeMillis()
            )
            saveTranslation(record)
            return@withContext record
        }

        val existing = findMatchingSeedOrSaved(trimmed, sourceLang, targetLang)
        if (existing != null) {
            return@withContext existing
        }

        // Local deterministic lexical & transliteration engine
        val translit = generateLatinTransliteration(trimmed)
        val meaningHi: String
        val meaningEn: String
        val partOfSpeech: String
        val examples = mutableListOf<String>()
        val synonyms = mutableListOf<String>()
        val antonyms = mutableListOf<String>()

        if (sourceLang == "hi") {
            meaningHi = "$trimmed (हिंदी शब्द)"
            meaningEn = "Translation of $trimmed [Lexical lookup]"
            partOfSpeech = "संज्ञा / विशेषण"
            examples.add("यह $trimmed का एक सुंदर उदाहरण है।")
            examples.add("This is an example sentence featuring $trimmed.")
            synonyms.add("$trimmed-रूप")
        } else {
            meaningHi = "$trimmed का हिंदी अर्थ"
            meaningEn = "Definition of $trimmed"
            partOfSpeech = "Noun / Adjective"
            examples.add("Using $trimmed in everyday conversation.")
            synonyms.add("Alternative meaning")
        }

        val record = TranslationRecord(
            id = UUID.randomUUID().toString(),
            sourceText = trimmed,
            sourceLang = sourceLang,
            targetLang = targetLang,
            translatedText = if (sourceLang == "hi") meaningEn else meaningHi,
            transliteration = translit,
            meaningHi = meaningHi,
            meaningEn = meaningEn,
            partOfSpeech = partOfSpeech,
            exampleSentences = examples,
            synonyms = synonyms,
            antonyms = antonyms,
            provider = "AnkShabd Local Lexicon",
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )

        // Auto-save translation record to history
        saveTranslation(record)
        record
    }

    private fun extractNumberTokens(input: String): List<String> {
        val normalized = buildString {
            for (ch in input) {
                when (ch) {
                    in '०'..'९' -> append(('0'.code + (ch.code - '०'.code)).toChar())
                    else -> append(ch)
                }
            }
        }
        return normalized
            .split(Regex("[^0-9]+"))
            .filter(String::isNotBlank)
    }

    private fun splitIntoTwoDigitSegments(token: String): List<String> = buildList {
        var index = 0
        while (index < token.length) {
            add(token.substring(index, (index + 2).coerceAtMost(token.length)))
            index += 2
        }
    }

    private fun findLocalWordsForCode(code: String): List<String> =
        SeedDictionary.SEED_ENTRIES.filter { it.expectedCode == code }.map { it.word }

    private suspend fun findMatchingSeedOrSaved(text: String, sourceLang: String, targetLang: String): TranslationRecord? {
        val seed = SeedDictionary.SEED_ENTRIES.find {
            if (sourceLang == "hi") it.word.equals(text, ignoreCase = true)
            else it.meaningEn.contains(text, ignoreCase = true)
        }
        if (seed != null) {
            val enc = PhoneticMatraEncoder.encode(seed.word)
            return TranslationRecord(
                id = UUID.randomUUID().toString(),
                sourceText = if (sourceLang == "hi") seed.word else text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                translatedText = if (sourceLang == "hi") seed.meaningEn else seed.word,
                transliteration = seed.transliteration,
                meaningHi = seed.meaningHi,
                meaningEn = seed.meaningEn,
                partOfSpeech = seed.category,
                exampleSentences = listOfNotNull(seed.exampleSentenceHi, seed.exampleSentenceEn),
                synonyms = listOf("${seed.word} (कोड ${enc.code})"),
                provider = "V2.3 Seed Lexicon",
                isFavorite = false,
                createdAt = System.currentTimeMillis()
            )
        }
        return null
    }

    suspend fun getSavedTranslations(query: String = ""): List<TranslationRecord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TranslationRecord>()
        val db = dbHelper.readableDatabase
        val selection = if (query.isNotBlank()) "source_text LIKE ? OR translated_text LIKE ? OR meaning_hi LIKE ?" else null
        val args = if (query.isNotBlank()) arrayOf("%$query%", "%$query%", "%$query%") else null

        val cursor = db.query("translation_records", null, selection, args, null, null, "created_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTranslation(it))
            }
        }
        list
    }

    suspend fun saveTranslation(record: TranslationRecord): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", record.id)
            put("source_text", record.sourceText)
            put("source_lang", record.sourceLang)
            put("target_lang", record.targetLang)
            put("translated_text", record.translatedText)
            put("transliteration", record.transliteration)
            put("meaning_hi", record.meaningHi)
            put("meaning_en", record.meaningEn)
            put("part_of_speech", record.partOfSpeech)
            put("example_sentences", JSONArray(record.exampleSentences).toString())
            put("synonyms", JSONArray(record.synonyms).toString())
            put("antonyms", JSONArray(record.antonyms).toString())
            put("provider", record.provider)
            put("is_favorite", if (record.isFavorite) 1 else 0)
            put("created_at", record.createdAt)
        }
        db.insertWithOnConflict("translation_records", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun deleteTranslation(id: String): Boolean = withContext(Dispatchers.IO) {
        val rows = dbHelper.writableDatabase.delete("translation_records", "id = ?", arrayOf(id))
        rows > 0
    }

    suspend fun toggleFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.execSQL("UPDATE translation_records SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END WHERE id = ?", arrayOf(id))
        true
    }

    suspend fun saveTranslationToNotes(record: TranslationRecord, notebookId: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", UUID.randomUUID().toString())
            put("notebook_id", notebookId)
            put("title", "अनुवाद: ${record.sourceText}")
            put("content", "### स्रोत: ${record.sourceText}\n**अनुवाद:** ${record.translatedText}\n**उच्चारण:** ${record.transliteration}\n\n**अर्थ (हिंदी):** ${record.meaningHi}\n**अर्थ (अंग्रेजी):** ${record.meaningEn}\n\n**वाक्य प्रयोग:**\n${record.exampleSentences.joinToString("\n") { "- $it" }}")
            put("template_type", "VOCABULARY")
            put("target_word", record.sourceText)
            put("tags", JSONArray(listOf("translation", record.sourceLang, record.targetLang)).toString())
            put("is_favorite", 0)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        db.insert("notes", null, cv)
    }

    private fun generateLatinTransliteration(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            val latin = when (ch) {
                'क' -> "k"; 'ख' -> "kh"; 'ग' -> "g"; 'घ' -> "gh"
                'च' -> "ch"; 'छ' -> "chh"; 'ज' -> "j"; 'झ' -> "jh"
                'ट' -> "t"; 'ठ' -> "th"; 'ड' -> "d"; 'ढ' -> "dh"; 'ण' -> "n"
                'त' -> "t"; 'थ' -> "th"; 'द' -> "d"; 'ध' -> "dh"; 'न' -> "n"
                'प' -> "p"; 'फ' -> "ph"; 'ब' -> "b"; 'भ' -> "bh"; 'म' -> "m"
                'य' -> "y"; 'र' -> "r"; 'ल' -> "l"; 'व' -> "v"
                'श' -> "sh"; 'ष' -> "sh"; 'स' -> "s"; 'ह' -> "h"
                'ा' -> "aa"; 'ि' -> "i"; 'ी' -> "ee"; 'ु' -> "u"; 'ू' -> "oo"
                'े' -> "e"; 'ै' -> "ai"; 'ो' -> "o"; 'ौ' -> "au"; 'ं' -> "n"
                'अ' -> "a"; 'आ' -> "aa"; 'इ' -> "i"; 'ई' -> "ee"; 'उ' -> "u"; 'ऊ' -> "oo"
                'ए' -> "e"; 'ऐ' -> "ai"; 'ओ' -> "o"; 'औ' -> "au"
                else -> ch.toString()
            }
            sb.append(latin)
        }
        return sb.toString()
    }

    private fun cursorToTranslation(cursor: android.database.Cursor): TranslationRecord {
        return TranslationRecord(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            sourceText = cursor.getString(cursor.getColumnIndexOrThrow("source_text")),
            sourceLang = cursor.getString(cursor.getColumnIndexOrThrow("source_lang")),
            targetLang = cursor.getString(cursor.getColumnIndexOrThrow("target_lang")),
            translatedText = cursor.getString(cursor.getColumnIndexOrThrow("translated_text")),
            transliteration = cursor.getString(cursor.getColumnIndexOrThrow("transliteration")),
            meaningHi = cursor.getString(cursor.getColumnIndexOrThrow("meaning_hi")),
            meaningEn = cursor.getString(cursor.getColumnIndexOrThrow("meaning_en")),
            partOfSpeech = cursor.getString(cursor.getColumnIndexOrThrow("part_of_speech")),
            exampleSentences = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("example_sentences"))),
            synonyms = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("synonyms"))),
            antonyms = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("antonyms"))),
            provider = cursor.getString(cursor.getColumnIndexOrThrow("provider")),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }

    private fun parseJsonArray(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
