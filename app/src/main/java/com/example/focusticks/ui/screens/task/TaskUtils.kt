package com.example.focusticks.ui.screens.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focusticks.TaskReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TaskUtils {

    fun parseDueMillis(input: String): Long? {
        val patterns = listOf(
            "MM/dd/yyyy hh:mm a",
            "MM/dd/yyyy HH:mm",
            "M/d/yyyy HH:mm",
            "MM/dd/yyyy"
        )
        for (p in patterns) {
            try {
                val d = SimpleDateFormat(p, Locale.US).apply { isLenient = false }.parse(input)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun isDueToday(due: String): Boolean {
        val ms = parseDueMillis(due) ?: return false
        val calDue = Calendar.getInstance().apply { timeInMillis = ms }
        val calNow = Calendar.getInstance()
        return calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
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
                val t = dueMillis - 24 * 60 * 60 * 1000
                scheduleSingle(context, "${taskId}_g", title, "gentle", t)
            }

            "moderate" -> {
                val t = dueMillis - 3 * 60 * 60 * 1000
                scheduleSingle(context, "${taskId}_m", title, "moderate", t)
            }

            "urgent" -> {
                scheduleSingle(context, "${taskId}_u1", title, "urgent", dueMillis - 30 * 60 * 1000)
                scheduleSingle(context, "${taskId}_u2", title, "urgent", dueMillis - 15 * 60 * 1000)
                scheduleSingle(context, "${taskId}_u3", title, "urgent", dueMillis - 5 * 60 * 1000)
            }
        }
    }

    fun cancelAllReminders(context: Context, taskId: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val ids = listOf(
            "${taskId}_g",
            "${taskId}_m",
            "${taskId}_u1",
            "${taskId}_u2",
            "${taskId}_u3"
        )
        ids.forEach { id ->
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarm.cancel(pending)
        }
    }
}
