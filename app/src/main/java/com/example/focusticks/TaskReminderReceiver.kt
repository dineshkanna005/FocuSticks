package com.example.focusticks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task Reminder"
        val taskId = intent.getStringExtra("taskId") ?: ""
        NotificationHelper.showReminderNotification(context, title, taskId)
    }
}
