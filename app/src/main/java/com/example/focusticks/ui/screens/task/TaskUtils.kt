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

    val formats = listOf(
        "MM/dd/yyyy hh:mm a",
        "MM/dd/yyyy HH:mm",
        "MM/dd/yyyy"
    )

    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.isLenient = false
            val date = sdf.parse(input)
            if (date != null) return date.time
        } catch (_: Exception) { }
    }

    return null
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleReminder(
    context: Context,
    taskId: String,
    title: String,
    due: String,
    remindBefore: Long?
) {
    val dueMillis = parseDueMillis(due) ?: return
    val before = (remindBefore ?: 0L) * 60000L
    val trigger = dueMillis - before
    if (trigger <= System.currentTimeMillis()) return

    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("taskId", taskId)
        putExtra("title", "$title is due soon")
    }

    val pending = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val m = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (m.canScheduleExactAlarms()) {
            m.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    } else {
        m.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }
}
