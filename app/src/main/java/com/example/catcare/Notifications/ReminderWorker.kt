package com.example.catcare.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, 0L)
        val title = inputData.getString(KEY_TITLE) ?: "Reminder"
        val text = inputData.getString(KEY_TEXT) ?: "Reminder"
        val repeatMins = inputData.getInt(KEY_REPEAT_MINUTES, 0)

        // post the notification
        postReminderNotification(applicationContext, id, title, text)

        // re-schedule if repeating
        if (repeatMins > 0) {
            val next = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(repeatMins.toLong(), TimeUnit.MINUTES)
                .addTag(TAG_V2)
                .addTag(tagFor(id))
                .build()

            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(workNameFor(id), ExistingWorkPolicy.REPLACE, next)
        }

        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_REPEAT_MINUTES = "repeat"

        fun tagFor(id: Long) = "catcare-reminder-$id"
        fun workNameFor(id: Long) = "catcare-reminder-work-$id"
    }
}