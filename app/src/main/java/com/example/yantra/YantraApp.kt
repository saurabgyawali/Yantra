package com.example.yantra

import android.app.Application
import com.example.yantra.notifications.NotificationHelper
import com.example.yantra.notifications.ReviewReminderScheduler

class YantraApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1) Create the notification channel once
        NotificationHelper.createNotificationChannel(this)

        // 2) Schedule the daily background work (runs once per day)
        ReviewReminderScheduler.scheduleDailyReviewReminder(this)
    }
}
