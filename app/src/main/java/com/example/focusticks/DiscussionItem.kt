package com.example.focusticks

import com.google.firebase.Timestamp

data class DiscussionItem(
    val id: String = "",
    val topicId: String = "",
    val text: String = "",
    val uid: String = "",
    val userName: String = "",
    val timestamp: Timestamp? = null,
    val replyToId: String? = null,
    val edited: Boolean = false,
    val editedAt: Timestamp? = null
)