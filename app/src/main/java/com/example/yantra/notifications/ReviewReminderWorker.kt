package com.example.yantra.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.yantra.data.repository.FlashcardRepository

class ReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Use your existing repository
            val repo = FlashcardRepository(applicationContext)
            val now = System.currentTimeMillis()

            // Count how many cards are due
            val dueCount = repo.countDue(now)

            if (dueCount > 0) {
                // Show a notification if there is something to review
                NotificationHelper.showReviewReminderNotification(
                    context = applicationContext,
                    dueCount = dueCount
                )
            }

            Result.success()
        } catch (e: Exception) {
            // If something goes wrong (DB error etc.), just fail gracefully
            Result.failure()
        }
    }
}
