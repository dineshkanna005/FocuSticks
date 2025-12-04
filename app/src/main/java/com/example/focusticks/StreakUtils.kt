package com.example.focusticks

import java.util.Calendar
import java.util.concurrent.TimeUnit

fun calculateStreak(lastCompletedMillis: Long): Int {
    if (lastCompletedMillis == 0L) return 0

    val now = Calendar.getInstance()
    val last = Calendar.getInstance().apply { timeInMillis = lastCompletedMillis }

    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)

    last.set(Calendar.HOUR_OF_DAY, 0)
    last.set(Calendar.MINUTE, 0)
    last.set(Calendar.SECOND, 0)
    last.set(Calendar.MILLISECOND, 0)

    val diff = now.timeInMillis - last.timeInMillis
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when (days) {
        0L -> 1
        1L -> 2
        else -> 0
    }
}
