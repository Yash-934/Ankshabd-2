package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.models.Flashcard
import com.pocketdev.ank_shabd.domain.models.ReviewRecord
import com.pocketdev.ank_shabd.domain.models.RevisionStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class RevisionRepository(context: Context) {

    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)

    suspend fun getDueFlashcards(limit: Int = 20): List<Flashcard> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Flashcard>()
        val db = dbHelper.readableDatabase
        val now = System.currentTimeMillis()

        // Fetch cards where next_review_date <= now, or unreviewed cards
        val cursor = db.query(
            "flashcards",
            null,
            "next_review_date <= ? OR repetitions = 0",
            arrayOf(now.toString()),
            null,
            null,
            "next_review_date ASC, streak ASC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToFlashcard(it))
            }
        }
        // If not enough due cards, fetch any cards to practice
        if (list.size < 5) {
            val extraCursor = db.query("flashcards", null, null, null, null, null, "RANDOM()", (limit - list.size).toString())
            extraCursor.use {
                while (it.moveToNext()) {
                    val card = cursorToFlashcard(it)
                    if (list.none { existing -> existing.id == card.id }) {
                        list.add(card)
                    }
                }
            }
        }
        list
    }

    suspend fun recordReview(cardId: String, rating: Int, timeSpentMs: Long = 0L): Flashcard = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()

        // Log review record
        val reviewId = UUID.randomUUID().toString()
        val rCv = ContentValues().apply {
            put("id", reviewId)
            put("card_id", cardId)
            put("timestamp", now)
            put("rating", rating)
            put("time_spent_ms", timeSpentMs)
        }
        db.insert("review_records", null, rCv)

        // Fetch current card state
        val cursor = db.query("flashcards", null, "id = ?", arrayOf(cardId), null, null, null, "1")
        val card = cursor.use {
            if (it.moveToNext()) cursorToFlashcard(it) else throw IllegalArgumentException("Card not found: $cardId")
        }

        // Apply SM-2 Spaced Repetition Formula
        // rating: 0 = Again, 1 = Hard, 2 = Good, 3 = Easy
        val isCorrect = rating >= 2
        val newStreak = if (isCorrect) card.streak + 1 else 0
        val newRepetitions = if (isCorrect) card.repetitions + 1 else 0

        // Quality q mapped to SM-2 scale (0 to 5)
        val q = when (rating) {
            0 -> 1 // Again
            1 -> 2 // Hard
            2 -> 4 // Good
            3 -> 5 // Easy
            else -> 3
        }

        // Calculate new Ease Factor (EF)
        var newEf = card.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (newEf < 1.3f) newEf = 1.3f

        // Calculate next interval in days
        val newIntervalDays = when (newRepetitions) {
            0 -> 1
            1 -> 1
            2 -> 6
            else -> (card.intervalDays * newEf).toInt().coerceAtLeast(1)
        }

        val millisInDay = 24 * 60 * 60 * 1000L
        val nextReviewDate = now + (newIntervalDays * millisInDay)

        val updatedCard = card.copy(
            repetitions = newRepetitions,
            intervalDays = newIntervalDays,
            easeFactor = newEf,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = now,
            timesCorrect = card.timesCorrect + (if (isCorrect) 1 else 0),
            timesIncorrect = card.timesIncorrect + (if (!isCorrect) 1 else 0),
            streak = newStreak
        )

        val cv = ContentValues().apply {
            put("repetitions", updatedCard.repetitions)
            put("interval_days", updatedCard.intervalDays)
            put("ease_factor", updatedCard.easeFactor)
            put("next_review_date", updatedCard.nextReviewDate)
            put("last_reviewed_date", updatedCard.lastReviewedDate)
            put("times_correct", updatedCard.timesCorrect)
            put("times_incorrect", updatedCard.timesIncorrect)
            put("streak", updatedCard.streak)
        }
        db.update("flashcards", cv, "id = ?", arrayOf(cardId))
        updatedCard
    }

    suspend fun getRevisionStats(): RevisionStats = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val now = System.currentTimeMillis()

        var totalCards = 0
        var dueToday = 0
        var learnedCount = 0
        var totalCorrect = 0
        var totalAttempts = 0
        var maxStreak = 0
        val difficultWords = mutableListOf<String>()

        val cursor = db.rawQuery("SELECT * FROM flashcards", null)
        cursor.use {
            totalCards = it.count
            while (it.moveToNext()) {
                val nextReview = it.getLong(it.getColumnIndexOrThrow("next_review_date"))
                val reps = it.getInt(it.getColumnIndexOrThrow("repetitions"))
                val correct = it.getInt(it.getColumnIndexOrThrow("times_correct"))
                val incorrect = it.getInt(it.getColumnIndexOrThrow("times_incorrect"))
                val streak = it.getInt(it.getColumnIndexOrThrow("streak"))
                val answer = it.getString(it.getColumnIndexOrThrow("answer"))

                if (nextReview <= now || reps == 0) dueToday++
                if (reps >= 2) learnedCount++
                totalCorrect += correct
                totalAttempts += (correct + incorrect)
                if (streak > maxStreak) maxStreak = streak
                if (incorrect > correct && !difficultWords.contains(answer)) {
                    difficultWords.add(answer)
                }
            }
        }

        val accuracy = if (totalAttempts > 0) (totalCorrect.toFloat() / totalAttempts.toFloat()) * 100f else 0f

        RevisionStats(
            totalCards = totalCards,
            dueToday = dueToday,
            learnedCount = learnedCount,
            accuracyPercentage = accuracy,
            currentStreak = maxStreak,
            difficultWords = difficultWords.take(5)
        )
    }

    private fun cursorToFlashcard(cursor: android.database.Cursor): Flashcard {
        return Flashcard(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            cardType = cursor.getString(cursor.getColumnIndexOrThrow("card_type")),
            prompt = cursor.getString(cursor.getColumnIndexOrThrow("prompt")),
            answer = cursor.getString(cursor.getColumnIndexOrThrow("answer")),
            options = parseJsonArray(cursor.getString(cursor.getColumnIndexOrThrow("options"))),
            explanation = cursor.getString(cursor.getColumnIndexOrThrow("explanation")),
            wordId = cursor.getString(cursor.getColumnIndexOrThrow("word_id")),
            code = cursor.getString(cursor.getColumnIndexOrThrow("code")),
            repetitions = cursor.getInt(cursor.getColumnIndexOrThrow("repetitions")),
            intervalDays = cursor.getInt(cursor.getColumnIndexOrThrow("interval_days")),
            easeFactor = cursor.getFloat(cursor.getColumnIndexOrThrow("ease_factor")),
            nextReviewDate = cursor.getLong(cursor.getColumnIndexOrThrow("next_review_date")),
            lastReviewedDate = cursor.getLong(cursor.getColumnIndexOrThrow("last_reviewed_date")),
            timesCorrect = cursor.getInt(cursor.getColumnIndexOrThrow("times_correct")),
            timesIncorrect = cursor.getInt(cursor.getColumnIndexOrThrow("times_incorrect")),
            streak = cursor.getInt(cursor.getColumnIndexOrThrow("streak"))
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
