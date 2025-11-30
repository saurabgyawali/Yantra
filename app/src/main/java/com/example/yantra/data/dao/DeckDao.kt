package com.example.yantra.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.yantra.data.model.Deck

@Dao
interface DeckDao {

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck): Int

    @Delete
    suspend fun delete(deck: Deck)

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<Deck>
}
