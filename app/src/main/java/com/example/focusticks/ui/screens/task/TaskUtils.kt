package com.example.focusticks.ui.screens.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focusticks.TaskReminderReceiver
import java.text.SimpleDateFormat
import java.util.Locale

fun parseDueMillis(input: String): Long? {
    val patterns = listOf(
        "MM/dd/yyyy hh:mm a",
        "MM/dd/yyyy HH:mm",
        "MM/dd/yyyy"
    )
    for (p in patterns) {
        try {
            val d = SimpleDateFormat(p, Locale.US).apply { isLenient = false }.parse(input)
            if (d != null) return d.time
        } catch (_: Exception) {}
    }
    return null
}

fun scheduleMultiReminder(
    context: Context,
    taskId: String,
    title: String,
    due: String,
    urgency: String
) {
    val dueMillis = parseDueMillis(due) ?: return

    when (urgency) {
        "gentle" -> {
            val trigger = dueMillis - 24 * 60 * 60 * 1000
            scheduleSingle(context, "${taskId}_g", title, "gentle", trigger)
        }
        "moderate" -> {
            val trigger = dueMillis - 3 * 60 * 60 * 1000
            scheduleSingle(context, "${taskId}_m", title, "moderate", trigger)
        }
        "urgent" -> {
            val t1 = dueMillis - 30 * 60 * 1000
            val t2 = dueMillis - 15 * 60 * 1000
            val t3 = dueMillis - 5 * 60 * 1000
            scheduleSingle(context, "${taskId}_u1", title, "urgent", t1)
            scheduleSingle(context, "${taskId}_u2", title, "urgent", t2)
            scheduleSingle(context, "${taskId}_u3", title, "urgent", t3)
        }
    }
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleSingle(
    context: Context,
    uniqueId: String,
    title: String,
    urgency: String,
    triggerMillis: Long
) {
    if (triggerMillis <= System.currentTimeMillis()) return

    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("taskId", uniqueId)
        putExtra("title", title)
        putExtra("urgency", urgency)
    }

    val pending = PendingIntent.getBroadcast(
        context,
        uniqueId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        }
    } else {
        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
    }
}
