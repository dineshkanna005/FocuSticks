package com.example.focusticks.ui.screens.task

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val category: String = "",
    val difficulty: String = "",
    val due: String = "",
    val urgency: String = "gentle",
    val completed: Boolean = false,
    val completedAt: String = "",
    val imageUrl: String = "",
    val fileUrl: String = ""
)
