package com.example.yantra.data.dao

import androidx.room.*
import com.example.yantra.data.model.Flashcard

@Dao
interface FlashcardDao {

    @Insert
    suspend fun insert(flashcard: Flashcard): Long

    @Update
    suspend fun update(flashcard: Flashcard): Int

    @Delete
    suspend fun delete(flashcard: Flashcard)

    // ───── Existing "global" queries ─────

    @Query("SELECT * FROM flashcards")
    suspend fun getAll(): List<Flashcard>

    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :time")
    suspend fun getDue(time: Long): List<Flashcard>

    @Query("SELECT COUNT(*) FROM flashcards")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE nextReviewAt <= :time")
    suspend fun countDue(time: Long): Int

    // ───── NEW: deck-specific queries ─────

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun getAllForDeck(deckId: Long): List<Flashcard>

    @Query(
        "SELECT * FROM flashcards " +
                "WHERE deckId = :deckId AND nextReviewAt <= :time"
    )
    suspend fun getDueForDeck(deckId: Long, time: Long): List<Flashcard>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    suspend fun countAllForDeck(deckId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM flashcards " +
                "WHERE deckId = :deckId AND nextReviewAt <= :time"
    )
    suspend fun countDueForDeck(deckId: Long, time: Long): Int
}
