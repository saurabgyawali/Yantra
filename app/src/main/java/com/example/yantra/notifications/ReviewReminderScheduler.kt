package com.example.yantra.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReviewReminderScheduler {

    private const val WORK_NAME = "review_reminder_work"

    fun scheduleDailyReviewReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Run once every 24 hours.
        // Initial delay here is 15 minutes (minimum granularity).
        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // don't reschedule if it already exists
            request
        )
    }
}
