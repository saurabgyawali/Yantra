package com.example.yantra.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // NEW: which deck this card belongs to (0 = default / uncategorized)
    val deckId: Long = 0L,

    val front: String,
    val back: String,

    // SRS fields
    val intervalDays: Int = 1,
    val easeFactor: Double = 2.5,
    val repetition: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = System.currentTimeMillis()
)
