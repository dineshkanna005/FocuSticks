package com.example.focusticks.ui.screens.task

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val category: String = "",
    val difficulty: String = "",
    val due: String = "",
    val remindBeforeMinutes: Long = 0L,
    val completed: Boolean = false
)

