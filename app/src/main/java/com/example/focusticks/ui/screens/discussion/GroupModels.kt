package com.example.focusticks.ui.screens.discussion

data class GroupMessage(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String? = null
)

data class GroupItem(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val adminId: String = "",
    val members: List<String> = emptyList()
)
