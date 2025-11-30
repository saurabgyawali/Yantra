package com.example.yantra.data.repository

import android.content.Context
import com.example.yantra.data.db.AppDatabase
import com.example.yantra.data.model.Deck

class DeckRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.deckDao()

    suspend fun insert(deck: Deck): Long = dao.insert(deck)

    suspend fun update(deck: Deck): Int = dao.update(deck)

    suspend fun delete(deck: Deck) = dao.delete(deck)

    suspend fun getAll(): List<Deck> = dao.getAll()
}
