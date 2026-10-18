package com.pocketdev.ank_shabd.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.models.*
import org.json.JSONArray
import org.json.JSONObject

class AnkShabdDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ank_shabd.db"
        const val DATABASE_VERSION = 2

        @Volatile
        private var instance: AnkShabdDatabaseHelper? = null

        fun getInstance(context: Context): AnkShabdDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AnkShabdDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Peg Words Table
        db.execSQL(
            """
            CREATE TABLE peg_words (
                id TEXT PRIMARY KEY,
                word TEXT NOT NULL,
                normalized_word TEXT NOT NULL,
                code TEXT NOT NULL,
                ruleset_id TEXT NOT NULL,
                category TEXT NOT NULL,
                meaning_hi TEXT NOT NULL,
                meaning_en TEXT NOT NULL,
                transliteration TEXT NOT NULL,
                quality_grade TEXT NOT NULL,
                verification_status TEXT NOT NULL,
                verification_source TEXT NOT NULL,
                rejection_reason TEXT,
                visual_association TEXT,
                mnemonic_story TEXT NOT NULL DEFAULT '',
                encoding_breakdown TEXT NOT NULL DEFAULT '',
                encoder_version TEXT NOT NULL DEFAULT '2.3',
                dictionary_evidence TEXT NOT NULL DEFAULT '',
                commonness TEXT NOT NULL DEFAULT 'medium',
                confidence REAL NOT NULL DEFAULT 1.0,
                example_sentence_hi TEXT,
                example_sentence_en TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                tags TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_peg_code ON peg_words(code)")
        db.execSQL("CREATE INDEX idx_peg_word ON peg_words(word)")
        db.execSQL("CREATE INDEX idx_peg_grade ON peg_words(quality_grade)")
        db.execSQL("CREATE INDEX idx_peg_status ON peg_words(verification_status)")
        db.execSQL("CREATE INDEX idx_peg_fav ON peg_words(is_favorite)")

        // 2. Notebooks Table
        db.execSQL(
            """
            CREATE TABLE notebooks (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                color_hex TEXT NOT NULL,
                icon TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 3. Note Folders Table
        db.execSQL(
            """
            CREATE TABLE note_folders (
                id TEXT PRIMARY KEY,
                notebook_id TEXT NOT NULL,
                name TEXT NOT NULL,
                parent_folder_id TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 4. Notes Table
        db.execSQL(
            """
            CREATE TABLE notes (
                id TEXT PRIMARY KEY,
                notebook_id TEXT NOT NULL,
                folder_id TEXT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                template_type TEXT NOT NULL,
                target_number TEXT,
                target_word TEXT,
                tags TEXT NOT NULL DEFAULT '',
                is_favorite INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_notebook ON notes(notebook_id)")
        db.execSQL("CREATE INDEX idx_notes_target_number ON notes(target_number)")
        db.execSQL("CREATE INDEX idx_notes_target_word ON notes(target_word)")

        // 5. Word-Note Relations Table
        db.execSQL(
            """
            CREATE TABLE word_note_relations (
                word_id TEXT NOT NULL,
                note_id TEXT NOT NULL,
                PRIMARY KEY (word_id, note_id)
            )
            """.trimIndent()
        )

        // 6. Translation Records Table
        db.execSQL(
            """
            CREATE TABLE translation_records (
                id TEXT PRIMARY KEY,
                source_text TEXT NOT NULL,
                source_lang TEXT NOT NULL,
                target_lang TEXT NOT NULL,
                translated_text TEXT NOT NULL,
                transliteration TEXT NOT NULL,
                meaning_hi TEXT NOT NULL,
                meaning_en TEXT NOT NULL,
                part_of_speech TEXT NOT NULL,
                example_sentences TEXT NOT NULL,
                synonyms TEXT NOT NULL,
                antonyms TEXT NOT NULL,
                provider TEXT NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 7. Memory Stories Table
        db.execSQL(
            """
            CREATE TABLE memory_stories (
                id TEXT PRIMARY KEY,
                number_sequence TEXT NOT NULL,
                code_segments TEXT NOT NULL,
                peg_words TEXT NOT NULL,
                story_title TEXT NOT NULL,
                story_hi TEXT NOT NULL,
                story_en TEXT,
                visual_scene TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                note_id TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 8. Flashcards Table
        db.execSQL(
            """
            CREATE TABLE flashcards (
                id TEXT PRIMARY KEY,
                card_type TEXT NOT NULL,
                prompt TEXT NOT NULL,
                answer TEXT NOT NULL,
                options TEXT NOT NULL,
                explanation TEXT NOT NULL,
                word_id TEXT,
                code TEXT,
                repetitions INTEGER NOT NULL DEFAULT 0,
                interval_days INTEGER NOT NULL DEFAULT 1,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                next_review_date INTEGER NOT NULL,
                last_reviewed_date INTEGER,
                times_correct INTEGER NOT NULL DEFAULT 0,
                times_incorrect INTEGER NOT NULL DEFAULT 0,
                streak INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // 9. Review Records Table
        db.execSQL(
            """
            CREATE TABLE review_records (
                id TEXT PRIMARY KEY,
                card_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                rating INTEGER NOT NULL,
                time_spent_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 10. Rulesets Table
        db.execSQL(
            """
            CREATE TABLE rulesets (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                description TEXT NOT NULL,
                is_default INTEGER NOT NULL DEFAULT 0,
                is_custom INTEGER NOT NULL DEFAULT 0,
                ruleset_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Seed initial data
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ensureColumnsAndSeedEntries(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            ensureColumnsAndSeedEntries(db)
        }
    }

    private fun ensureColumnsAndSeedEntries(db: SQLiteDatabase) {
        val existingColumns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(peg_words)", null).use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext() && nameIdx >= 0) {
                existingColumns.add(cursor.getString(nameIdx))
            }
        }
        if ("mnemonic_story" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN mnemonic_story TEXT NOT NULL DEFAULT ''")
        }
        if ("encoding_breakdown" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN encoding_breakdown TEXT NOT NULL DEFAULT ''")
        }
        if ("encoder_version" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN encoder_version TEXT NOT NULL DEFAULT '2.3'")
        }
        if ("dictionary_evidence" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN dictionary_evidence TEXT NOT NULL DEFAULT ''")
        }
        if ("commonness" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN commonness TEXT NOT NULL DEFAULT 'medium'")
        }
        if ("confidence" !in existingColumns) {
            db.execSQL("ALTER TABLE peg_words ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
        }

        // Insert any new seed entries without overwriting existing records
        val now = System.currentTimeMillis()
        val validatedPegs = SeedDictionary.getValidatedSeedPegs(EncodingRuleSet.V2_3)
        for (peg in validatedPegs) {
            val pCv = pegToContentValues(peg, now)
            db.insertWithOnConflict("peg_words", null, pCv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun pegToContentValues(peg: PegWord, now: Long): ContentValues {
        return ContentValues().apply {
            put("id", peg.id)
            put("word", peg.word)
            put("normalized_word", peg.normalizedWord)
            put("code", peg.code)
            put("ruleset_id", peg.ruleSetId)
            put("category", peg.category)
            put("meaning_hi", peg.meaningHi)
            put("meaning_en", peg.meaningEn)
            put("transliteration", peg.transliteration)
            put("quality_grade", peg.qualityGrade)
            put("verification_status", peg.verificationStatus)
            put("verification_source", peg.verificationSource)
            put("rejection_reason", peg.rejectionReason)
            put("visual_association", peg.visualAssociation)
            put("mnemonic_story", peg.mnemonicStory ?: "")
            put("encoding_breakdown", peg.encodingBreakdown ?: "")
            put("encoder_version", peg.encoderVersion)
            put("dictionary_evidence", peg.dictionaryEvidence ?: "")
            put("commonness", peg.commonness ?: "medium")
            put("confidence", peg.confidence ?: 1.0f)
            put("example_sentence_hi", peg.exampleSentenceHi)
            put("example_sentence_en", peg.exampleSentenceEn)
            put("is_favorite", if (peg.isFavorite) 1 else 0)
            put("tags", JSONArray(peg.tags).toString())
            put("created_at", now)
            put("updated_at", now)
        }
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Seed Starter Notebooks
        val starterNotebooks = listOf(
            Triple("Number Memory System", "संख्या स्मृति प्रणाली एवं मास्टर कोड", "#1E3A8A"),
            Triple("Peg Word Dictionary", "पेग शब्दकोश और संदर्भ प्रविष्टियाँ", "#0D9488"),
            Triple("Hindi–English Vocabulary", "हिंदी–अंग्रेजी शब्दावली एवं अर्थ", "#D97706"),
            Triple("Number-to-Word Experiments", "अंक-से-शब्द नए प्रयोग", "#7C3AED"),
            Triple("Translation Notes", "अनुवाद एवं संदर्भ नोट्स", "#2563EB"),
            Triple("Mnemonic Stories", "याद रखने वाली दृश्य कहानियाँ", "#DC2626"),
            Triple("Learning and Revision", "दैनिक अभ्यास एवं पुनरावृत्ति नोट्स", "#059669"),
            Triple("Personal Notes", "व्यक्तिगत टिप्पणियाँ एवं विचार", "#475569")
        )

        for ((index, nb) in starterNotebooks.withIndex()) {
            val cv = ContentValues().apply {
                put("id", "starter_notebook_${index + 1}")
                put("name", nb.first)
                put("description", nb.second)
                put("color_hex", nb.third)
                put("icon", "book")
                put("created_at", now)
                put("updated_at", now)
            }
            db.insert("notebooks", null, cv)
        }

        // 2. Seed V2.3 Default Ruleset
        val ruleSetJson = JSONObject().apply {
            put("id", EncodingRuleSet.V2_3.id)
            put("name", EncodingRuleSet.V2_3.name)
            put("version", EncodingRuleSet.V2_3.version)
            put("description", EncodingRuleSet.V2_3.description)
        }.toString()

        val rCv = ContentValues().apply {
            put("id", EncodingRuleSet.V2_3.id)
            put("name", EncodingRuleSet.V2_3.name)
            put("version", EncodingRuleSet.V2_3.version)
            put("description", EncodingRuleSet.V2_3.description)
            put("is_default", 1)
            put("is_custom", 0)
            put("ruleset_json", ruleSetJson)
            put("created_at", now)
        }
        db.insert("rulesets", null, rCv)

        // 3. Seed Validated Peg Words from SeedDictionary
        val validatedPegs = SeedDictionary.getValidatedSeedPegs(EncodingRuleSet.V2_3)
        for (peg in validatedPegs) {
            val pCv = pegToContentValues(peg, now)
            db.insertWithOnConflict("peg_words", null, pCv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        // 4. Seed Initial Flashcards from top pegs (e.g. 01 to 20)
        val sampleCards = validatedPegs.filter { it.qualityGrade == "A" && it.code.length == 2 }.take(15)
        for (peg in sampleCards) {
            // Number to Hindi Peg card
            val fcCv = ContentValues().apply {
                put("id", "fc_num_${peg.code}_${peg.id}")
                put("card_type", "NUMBER_TO_WORD")
                put("prompt", "संख्या ${peg.code} का स्वीकृत पेग शब्द क्या है?")
                put("answer", peg.word)
                put("options", JSONArray(listOf(peg.word, "कमल", "दवा", "शहर", "खेल").distinct().take(4)).toString())
                put("explanation", "${peg.word} का कोड ${peg.code} है (${peg.meaningHi})")
                put("word_id", peg.id)
                put("code", peg.code)
                put("repetitions", 0)
                put("interval_days", 1)
                put("ease_factor", 2.5f)
                put("next_review_date", now)
                put("times_correct", 0)
                put("times_incorrect", 0)
                put("streak", 0)
            }
            db.insert("flashcards", null, fcCv)

            // Word to Number Code card
            val fcCv2 = ContentValues().apply {
                put("id", "fc_word_${peg.id}_${peg.code}")
                put("card_type", "WORD_TO_NUMBER")
                put("prompt", "शब्द '${peg.word}' का V2.3 नंबर कोड क्या है?")
                put("answer", peg.code)
                put("options", JSONArray(listOf(peg.code, "11", "18", "04", "28").distinct().take(4)).toString())
                put("explanation", "${peg.word} = ${peg.code}")
                put("word_id", peg.id)
                put("code", peg.code)
                put("repetitions", 0)
                put("interval_days", 1)
                put("ease_factor", 2.5f)
                put("next_review_date", now)
                put("times_correct", 0)
                put("times_incorrect", 0)
                put("streak", 0)
            }
            db.insert("flashcards", null, fcCv2)
        }
    }
}
