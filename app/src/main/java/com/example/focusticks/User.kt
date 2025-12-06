package com.example.focusticks

import com.google.firebase.firestore.Exclude

data class User(
    @get:Exclude val uid: String = "",
    val name: String = "",
    val studentId: String = "",
    val phoneNo: String = "",
    val email: String = "",
    val dob: String = "",
    val points: Long = 0L,
    val lastTaskCompleted: Long = 0L,
    val streakDays: Int = 0
)
