package com.example.focusticks

import java.util.Calendar
import java.util.concurrent.TimeUnit

fun calculateStreak(lastTaskCompleted: Long): Int {
    if (lastTaskCompleted == 0L) return 0

    val now = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
    val lastCompleted = Calendar.getInstance().apply { timeInMillis = lastTaskCompleted }

    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)

    lastCompleted.set(Calendar.HOUR_OF_DAY, 0)
    lastCompleted.set(Calendar.MINUTE, 0)
    lastCompleted.set(Calendar.SECOND, 0)
    lastCompleted.set(Calendar.MILLISECOND, 0)

    val diffInMillis = now.timeInMillis - lastCompleted.timeInMillis
    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return if (days == 0L) 1 else 0
}
