package com.example.focusticks.ui.screens.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.focusticks.TaskReminderReceiver
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

fun parseDueMillis(dueStr: String): Long? {
    val formats = listOf("MM/dd/yyyy HH:mm", "MM/dd/yyyy hh:mm a")
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.isLenient = false
            return sdf.parse(dueStr)?.time
        } catch (_: ParseException) {}
    }
    return null
}

fun difficultyScore(d: String): Int {
    return when (d.trim().lowercase(Locale.US)) {
        "hard" -> 3
        "medium" -> 2
        "easy" -> 1
        else -> 0
    }
}

fun findHardestTaskDueSoon(tasks: List<TaskItem>): TaskItem? {
    val now = System.currentTimeMillis()
    val soon = now + TimeUnit.HOURS.toMillis(48)

    val candidates = tasks.mapNotNull { t ->
        val due = parseDueMillis(t.due) ?: return@mapNotNull null
        if (!t.completed && due in now..soon) t to due else null
    }

    if (candidates.isEmpty()) return null

    val best = candidates.maxWith(
        compareBy<Pair<TaskItem, Long>> { difficultyScore(it.first.difficulty) }
            .thenBy { -it.second }
    )

    return best.first
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleReminder(
    context: Context,
    taskId: String,
    title: String,
    due: String,
    remindBefore: Long?
) {
    val dueMillis = if (due.isBlank()) null else parseDueMillis(due)
    if (dueMillis == null && due.isNotBlank()) return

    val now = System.currentTimeMillis()
    val offset = (remindBefore ?: 0L) * 60000
    val base = dueMillis ?: now + 30000
    var triggerAt = base - offset
    if (triggerAt < now + 5000) triggerAt = now + 5000

    val perm = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!perm) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("title", title)
    }

    val pending = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        if (Build.VERSION.SDK_INT >= 31) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
        Log.d("TASKS", "Alarm set for $title at $triggerAt")
    } catch (e: Exception) {
        Log.e("TASKS", "Failed to schedule reminder", e)
    }
}

fun cancelReminder(context: Context, taskId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TaskReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pending)
}
