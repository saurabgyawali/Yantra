package com.example.yantra.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.yantra.data.dao.FlashcardDao
import com.example.yantra.data.dao.DeckDao
import com.example.yantra.data.model.Flashcard
import com.example.yantra.data.model.Deck

@Database(
    entities = [
        Flashcard::class,
        Deck::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun flashcardDao(): FlashcardDao
    abstract fun deckDao(): DeckDao   // NEW

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yantra.db"
                )
                    // You already had this; keeps things simple during development
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
