package com.example.focusticks

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val taskId = intent.getStringExtra("taskId") ?: return
        val title = intent.getStringExtra("title") ?: "Reminder"
        val urgency = intent.getStringExtra("urgency") ?: "gentle"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openTaskId", taskId)
            putExtra("openType", urgency)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "multi_reminders"
        val manager = context.getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        when (urgency) {
            "gentle" -> {
                builder.setPriority(NotificationCompat.PRIORITY_LOW)
                builder.setSilent(true)
            }
            "moderate" -> {
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                builder.setVibrate(longArrayOf(0, 400, 200, 400))
            }
            "urgent" -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setVibrate(longArrayOf(0, 700, 300, 700))
                builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            }
            "completed" -> {
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
                builder.setContentText("Great job! You finished a task.")
            }
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
