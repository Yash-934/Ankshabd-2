package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.models.Note
import com.pocketdev.ank_shabd.domain.models.Notebook
import com.pocketdev.ank_shabd.domain.models.NoteFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class NotesRepository(context: Context) {

    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)

    suspend fun getAllNotebooks(): List<Notebook> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Notebook>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("notebooks", null, null, null, null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Notebook(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        colorHex = it.getString(it.getColumnIndexOrThrow("color_hex")),
                        icon = it.getString(it.getColumnIndexOrThrow("icon")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                    )
                )
            }
        }
        list
    }

    suspend fun createNotebook(name: String, desc: String, color: String): Notebook = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val notebook = Notebook(id = id, name = name, description = desc, colorHex = color, createdAt = now, updatedAt = now)
        val cv = ContentValues().apply {
            put("id", notebook.id)
            put("name", notebook.name)
            put("description", notebook.description)
            put("color_hex", notebook.colorHex)
            put("icon", notebook.icon)
            put("created_at", notebook.createdAt)
            put("updated_at", notebook.updatedAt)
        }
        dbHelper.writableDatabase.insert("notebooks", null, cv)
        notebook
    }

    suspend fun getFolders(notebookId: String? = null): List<NoteFolder> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<NoteFolder>()
        val selection = if (notebookId.isNullOrBlank()) null else "notebook_id = ?"
        val args = if (selection == null) null else arrayOf(notebookId)
        dbHelper.readableDatabase.query(
            "note_folders",
            null,
            selection,
            args,
            null,
            null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                folders.add(
                    NoteFolder(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        notebookId = cursor.getString(cursor.getColumnIndexOrThrow("notebook_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        parentFolderId = cursor.getString(cursor.getColumnIndexOrThrow("parent_folder_id")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        folders
    }

    suspend fun saveFolder(folder: NoteFolder): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", folder.id)
            put("notebook_id", folder.notebookId)
            put("name", folder.name.trim())
            put("parent_folder_id", folder.parentFolderId)
            put("created_at", folder.createdAt)
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            "note_folders",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun renameFolder(id: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("name", name.trim()) }
        dbHelper.writableDatabase.update("note_folders", values, "id = ?", arrayOf(id)) > 0
    }

    suspend fun deleteFolder(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query("note_folders", arrayOf("parent_folder_id"), "id = ?", arrayOf(id), null, null, null, "1")
            val parentId = cursor.use { if (it.moveToFirst()) it.getString(0) else null }
            db.execSQL("UPDATE notes SET folder_id = ? WHERE folder_id = ?", arrayOf(parentId, id))
            db.execSQL("UPDATE note_folders SET parent_folder_id = ? WHERE parent_folder_id = ?", arrayOf(parentId, id))
            val rows = db.delete("note_folders", "id = ?", arrayOf(id))
            if (rows > 0) db.setTransactionSuccessful()
            rows > 0
        } finally {
            db.endTransaction()
        }
    }

    suspend fun deleteNotebook(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("notes", "notebook_id = ?", arrayOf(id))
            db.delete("note_folders", "notebook_id = ?", arrayOf(id))
            val rows = db.delete("notebooks", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
            rows > 0
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getNotes(notebookId: String? = null, query: String = ""): List<Note> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (!notebookId.isNullOrBlank()) {
            selectionParts.add("notebook_id = ?")
            selectionArgs.add(notebookId)
        }
        if (query.isNotBlank()) {
            val q = "%${query.trim()}%"
            selectionParts.add("(title LIKE ? OR content LIKE ? OR target_number LIKE ? OR target_word LIKE ? OR tags LIKE ?)")
            repeat(5) { selectionArgs.add(q) }
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor = db.query("notes", null, selection, args, null, null, "updated_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToNote(it))
            }
        }
        list
    }

    suspend fun getNoteById(id: String): Note? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("notes", null, "id = ?", arrayOf(id), null, null, null, "1")
        cursor.use {
            if (it.moveToNext()) cursorToNote(it) else null
        }
    }

    suspend fun saveNote(note: Note): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", note.id.ifBlank { UUID.randomUUID().toString() })
            put("notebook_id", note.notebookId)
            put("folder_id", note.folderId)
            put("title", note.title)
            put("content", note.content)
            put("template_type", note.templateType)
            put("target_number", note.targetNumber)
            put("target_word", note.targetWord)
            put("tags", JSONArray(note.tags).toString())
            put("is_favorite", if (note.isFavorite) 1 else 0)
            put("created_at", note.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun deleteNote(id: String): Boolean = withContext(Dispatchers.IO) {
        val rows = dbHelper.writableDatabase.delete("notes", "id = ?", arrayOf(id))
        rows > 0
    }

    suspend fun toggleFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.execSQL("UPDATE notes SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END, updated_at = ? WHERE id = ?", arrayOf(System.currentTimeMillis(), id))
        true
    }

    suspend fun getRecentNotes(limit: Int = 5): List<Note> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("notes", null, null, null, null, null, "updated_at DESC", limit.toString())
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToNote(it))
            }
        }
        list
    }

    private fun cursorToNote(cursor: android.database.Cursor): Note {
        return Note(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            notebookId = cursor.getString(cursor.getColumnIndexOrThrow("notebook_id")),
            folderId = cursor.getString(cursor.getColumnIndexOrThrow("folder_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
            templateType = cursor.getString(cursor.getColumnIndexOrThrow("template_type")),
            targetNumber = cursor.getString(cursor.getColumnIndexOrThrow("target_number")),
            targetWord = cursor.getString(cursor.getColumnIndexOrThrow("target_word")),
            tags = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("tags"))),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
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
