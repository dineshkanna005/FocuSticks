package com.example.focusticks

import com.google.firebase.firestore.Exclude

data class User(
    @get:Exclude val uid: String = "",
    val name: String = "",
    val bio: String = "",
    val education: String = "",
    val studentId: String = "",
    val phoneNo: String = "",
    val points: Long = 0L,
    val lastTaskCompleted: Long = 0L
)