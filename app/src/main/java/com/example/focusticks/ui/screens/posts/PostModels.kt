package com.example.focusticks.ui.screens.posts

data class PostItem(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList()
)

data class CommentItem(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val time: Long = 0L
)
