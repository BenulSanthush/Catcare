package com.example.catcare.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.catcare.R
import com.example.catcare.data.model.ReminderEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.max

// public constants
const val CHANNEL_ID = "catcare.reminders.v3"
const val TAG_V2 = "catcare-reminder-v2"
private const val GROUP_PREFIX = "catcare-reminder-" // unique per id

fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val nm = context.getSystemService(NotificationManager::class.java)


        listOf(
            "catcare.reminders",
            "CatCare Reminders",
            "PetCare Reminders",
            "reminders",
            "catcare-primary-ch"
        ).forEach {
            try { nm.deleteNotificationChannel(it) } catch (_: Throwable) {}
        }

        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val ch = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setShowBadge(true)
                setSound(sound, attrs)
                description = "CatCare reminders"
            }
            nm.createNotificationChannel(ch)
        }
    }
}

private fun dueText(r: ReminderEntity): String =
    r.dueAt?.let {
        val fmt = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a")
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(fmt)
    } ?: "Reminder"

fun postReminderNotification(ctx: Context, id: Long, title: String, text: String) {
    ensureReminderChannel(ctx)

    val note = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setOnlyAlertOnce(false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setGroup(GROUP_PREFIX + id) // each reminder in its own “group of 1”
        .build()

    try {
        NotificationManagerCompat.from(ctx).notify(id.toInt(), note)
    } catch (_: SecurityException) {

    }
}


fun scheduleReminder(context: Context, r: ReminderEntity) {
    ensureReminderChannel(context)

    val now = System.currentTimeMillis()
    val intervalMs = (r.repeatMinutes ?: 0) * 60_000L


    val firstAt = when {
        r.dueAt == null -> now
        r.dueAt > now -> r.dueAt
        intervalMs > 0 -> {
            val delta = now - r.dueAt
            val k = (delta + intervalMs - 1) / intervalMs
            r.dueAt + k * intervalMs
        }
        else -> now
    }

    val delayMs = max(0L, firstAt - now)
    val data = Data.Builder()
        .putLong(ReminderWorker.KEY_ID, r.id)
        .putString(ReminderWorker.KEY_TITLE, r.title)
        .putString(ReminderWorker.KEY_TEXT, dueText(r))
        .putInt(ReminderWorker.KEY_REPEAT_MINUTES, r.repeatMinutes ?: 0)
        .build()

    val req = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInputData(data)
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .addTag(TAG_V2)
        .addTag(ReminderWorker.tagFor(r.id))
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(ReminderWorker.workNameFor(r.id), ExistingWorkPolicy.REPLACE, req)
}

fun cancelReminder(context: Context, id: Long) {

    WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.workNameFor(id))
    try { NotificationManagerCompat.from(context).cancel(id.toInt()) } catch (_: Throwable) {}
}


fun purgeLegacyWork(context: Context) {
    val wm = WorkManager.getInstance(context)
    wm.cancelAllWorkByTag("catcare-reminder")
    wm.cancelAllWorkByTag("legacy")
    wm.pruneWork()
}