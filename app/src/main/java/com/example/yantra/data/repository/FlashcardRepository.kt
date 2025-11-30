package com.example.yantra.data.repository

import android.content.Context
import com.example.yantra.data.db.AppDatabase
import com.example.yantra.data.model.Flashcard

class FlashcardRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.flashcardDao()

    suspend fun insert(card: Flashcard): Long = dao.insert(card)
    suspend fun update(card: Flashcard) = dao.update(card)
    suspend fun delete(card: Flashcard) = dao.delete(card)

    // Global queries (all decks)
    suspend fun getAll(): List<Flashcard> = dao.getAll()
    suspend fun getDue(time: Long): List<Flashcard> = dao.getDue(time)
    suspend fun countAll(): Int = dao.countAll()
    suspend fun countDue(time: Long): Int = dao.countDue(time)

    // Deck-specific queries
    suspend fun getAllForDeck(deckId: Long): List<Flashcard> =
        dao.getAllForDeck(deckId)

    suspend fun getDueForDeck(deckId: Long, time: Long): List<Flashcard> =
        dao.getDueForDeck(deckId, time)

    suspend fun countAllForDeck(deckId: Long): Int =
        dao.countAllForDeck(deckId)

    suspend fun countDueForDeck(deckId: Long, time: Long): Int =
        dao.countDueForDeck(deckId, time)
}
