package com.example.yantra.data.srs

import com.example.yantra.data.model.Flashcard
import kotlin.math.max

enum class Rating { AGAIN, GOOD, EASY }

fun updateCardSRS(card: Flashcard, rating: Rating): Flashcard {
    val now = System.currentTimeMillis()

    var ef = card.easeFactor
    var interval = card.intervalDays
    var rep = card.repetition

    when (rating) {
        Rating.AGAIN -> {
            rep = 0
            interval = 1
            ef = max(1.3, ef - 0.2)
        }
        Rating.GOOD -> {
            rep += 1
            ef = max(1.3, ef)
            interval = when (rep) {
                1 -> 1
                2 -> 3
                else -> (interval * ef).toInt()
            }
        }
        Rating.EASY -> {
            rep += 1
            ef += 0.15
            interval = (interval * ef * 1.3).toInt()
        }
    }

    return card.copy(
        intervalDays = interval,
        easeFactor = ef,
        repetition = rep,
        nextReviewAt = now + interval * 24 * 60 * 60 * 1000L
    )
}
