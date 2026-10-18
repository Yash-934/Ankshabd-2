package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.models.MemoryStory
import com.pocketdev.ank_shabd.domain.models.Note
import com.pocketdev.ank_shabd.domain.models.PegWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class MemoryStoryRepository(
    private val context: Context,
    private val dictionaryRepository: DictionaryRepository,
    private val notesRepository: NotesRepository
) {
    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)

    suspend fun generateStory(
        numberSequence: String,
        allowBLevel: Boolean = true,
        customPegSelections: Map<String, String> = emptyMap()
    ): GeneratedStoryPackage = withContext(Dispatchers.IO) {
        // Parse segments from sequence e.g. "11 - 18 - 28" or "1857"
        val segments = parseSequenceIntoSegments(numberSequence)
        val pegWords = mutableListOf<String>()
        val missingSlots = mutableListOf<String>()
        val segmentMapping = mutableListOf<Pair<String, PegWord?>>()

        for (seg in segments) {
            val customWord = customPegSelections[seg]
            if (customWord != null) {
                val peg = dictionaryRepository.getPegByWord(customWord)
                pegWords.add(customWord)
                segmentMapping.add(seg to peg)
            } else {
                val candidates = dictionaryRepository.getPegsByCode(seg)
                val chosen = candidates.firstOrNull { it.qualityGrade == "A" && it.verificationStatus == "approved" }
                    ?: if (allowBLevel) candidates.firstOrNull { it.qualityGrade == "B" && it.verificationStatus == "approved" } else null
                    ?: candidates.firstOrNull()

                if (chosen != null) {
                    pegWords.add(chosen.word)
                    segmentMapping.add(seg to chosen)
                } else {
                    missingSlots.add(seg)
                    pegWords.add("[कोड $seg रिक्त]")
                    segmentMapping.add(seg to null)
                }
            }
        }

        // Construct Vivid Memory Scene
        val title = "संख्या $numberSequence की स्मृति कथा"
        val vividScene = buildVividScene(segments, pegWords)
        val shortScene = buildConciseScene(segments, pegWords)

        val memoryStory = MemoryStory(
            id = UUID.randomUUID().toString(),
            numberSequence = numberSequence,
            codeSegments = segments,
            pegWords = pegWords,
            storyTitle = title,
            storyHi = vividScene,
            storyEn = "A memorable mnemonic story linking objects: ${pegWords.joinToString(" -> ")}",
            visualScene = shortScene,
            isFavorite = false,
            createdAt = System.currentTimeMillis()
        )

        GeneratedStoryPackage(
            story = memoryStory,
            segments = segments,
            segmentMapping = segmentMapping,
            missingSlots = missingSlots,
            conciseAlternative = shortScene,
            vividAlternative = vividScene
        )
    }

    private fun parseSequenceIntoSegments(input: String): List<String> {
        val cleaned = input.trim()
        if (cleaned.contains("-") || cleaned.contains(" ") || cleaned.contains(",")) {
            return cleaned.split(Regex("[- ,]+")).filter { it.isNotBlank() }
        }
        // Continuous numeric string e.g. "1857" -> split into 2-digit chunks: "18", "57"
        val result = mutableListOf<String>()
        var i = 0
        while (i < cleaned.length) {
            if (i + 2 <= cleaned.length) {
                result.add(cleaned.substring(i, i + 2))
                i += 2
            } else {
                result.add(cleaned.substring(i))
                i++
            }
        }
        return result
    }

    private fun buildVividScene(segments: List<String>, words: List<String>): String {
        if (words.isEmpty()) return "कोई पेग शब्द उपलब्ध नहीं है।"
        val mappingStr = segments.indices.joinToString(", ") { "${segments[it]} → ${words[it]}" }

        val sb = StringBuilder()
        sb.append("दृश्य कल्पना ($mappingStr):\n\n")

        for (i in words.indices) {
            val curr = words[i]
            if (i == 0) {
                sb.append("सबसे पहले एक विशाल और अद्भुत **$curr** को अपनी आँखों के सामने देखें। ")
            } else {
                val prev = words[i - 1]
                sb.append("अचानक उस **$prev** के ऊपर से एक चमकीला **$curr** प्रकट होता है और तेज़ी से घूमने लगता है! ")
            }
        }
        sb.append("\n\nयह अकल्पनीय दृश्य आपके मन में हमेशा के लिए अंकित हो जाता है।")
        return sb.toString()
    }

    private fun buildConciseScene(segments: List<String>, words: List<String>): String {
        return words.joinToString(" ➔ ") { "**$it**" }
    }

    suspend fun saveStory(story: MemoryStory): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", story.id)
            put("number_sequence", story.numberSequence)
            put("code_segments", JSONArray(story.codeSegments).toString())
            put("peg_words", JSONArray(story.pegWords).toString())
            put("story_title", story.storyTitle)
            put("story_hi", story.storyHi)
            put("story_en", story.storyEn)
            put("visual_scene", story.visualScene)
            put("is_favorite", if (story.isFavorite) 1 else 0)
            put("note_id", story.noteId)
            put("created_at", story.createdAt)
        }
        db.insertWithOnConflict("memory_stories", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun exportStoryToNotes(story: MemoryStory, notebookId: String): Note = withContext(Dispatchers.IO) {
        val content = """
            # ${story.storyTitle}
            
            **संख्या श्रृंखला:** `${story.numberSequence}`
            
            ### पेग मैपिंग:
            ${story.codeSegments.indices.joinToString("\n") { "- **${story.codeSegments[it]}** → ${story.pegWords.getOrNull(it) ?: ""}" }}
            
            ### दृश्य कथा (Memory Scene):
            ${story.storyHi}
            
            *स्मृति सिनेमा द्वारा जनरेट किया गया*
        """.trimIndent()

        val note = Note(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            title = story.storyTitle,
            content = content,
            templateType = "NUMBER_MEMORY",
            targetNumber = story.numberSequence,
            tags = listOf("memory_story", "mnemonic", story.numberSequence)
        )
        notesRepository.saveNote(note)
        // Update story note link
        saveStory(story.copy(noteId = note.id))
        note
    }

    suspend fun getAllStories(): List<MemoryStory> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MemoryStory>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("memory_stories", null, null, null, null, null, "created_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
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
        list
    }

    suspend fun deleteStory(id: String): Boolean = withContext(Dispatchers.IO) {
        val rows = dbHelper.writableDatabase.delete("memory_stories", "id = ?", arrayOf(id))
        rows > 0
    }

    suspend fun toggleFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.execSQL("UPDATE memory_stories SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END WHERE id = ?", arrayOf(id))
        true
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

data class GeneratedStoryPackage(
    val story: MemoryStory,
    val segments: List<String>,
    val segmentMapping: List<Pair<String, PegWord?>>,
    val missingSlots: List<String>,
    val conciseAlternative: String,
    val vividAlternative: String
)
