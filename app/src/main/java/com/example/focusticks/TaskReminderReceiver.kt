package com.example.focusticks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task reminder"
        showNotificationNow(context, title)
        Log.d("TASKS", "Reminder fired for title=$title")
    }
}
