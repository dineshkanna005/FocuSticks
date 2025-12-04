package com.example.focusticks

import com.google.firebase.Timestamp

data class DiscussionItem(
    val id: String = "",
    val text: String = "",
    val uid: String = "",
    val userName: String = "",
    val timestamp: Timestamp? = null,
    val replyToId: String? = null
)
