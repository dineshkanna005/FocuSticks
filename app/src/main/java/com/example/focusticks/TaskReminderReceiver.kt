package com.example.focusticks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val taskId = intent.getStringExtra("taskId") ?: return
        val title = intent.getStringExtra("title") ?: "Task Reminder"

        NotificationHelper.showReminderNotification(
            context,
            "Reminder: $title",
            taskId,
            "reminder"
        )
    }
}
