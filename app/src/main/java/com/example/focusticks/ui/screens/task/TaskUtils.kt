package com.example.focusticks

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale

fun parseDueMillis(d: String): Long? {
    val formats = listOf(
        "MM/dd/yyyy HH:mm",
        "MM/dd/yyyy hh:mm a"
    )
    for (f in formats) {
        try {
            val sdf = SimpleDateFormat(f, Locale.US)
            sdf.isLenient = false
            val date = sdf.parse(d)
            if (date != null) return date.time
        } catch (_: Exception) {
        }
    }
    return null
}

fun difficultyScore(d: String): Int {
    return when (d.lowercase()) {
        "hard" -> 3
        "medium" -> 2
        "easy" -> 1
        else -> 0
    }
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleReminder(context: Context, taskId: String, title: String, due: String, remindBefore: Long?) {
    val dueMillis = parseDueMillis(due) ?: return
    val offset = (remindBefore ?: 0L) * 60000
    val triggerTime = dueMillis - offset
    if (triggerTime < System.currentTimeMillis()) return

    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("taskId", taskId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
