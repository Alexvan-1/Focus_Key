package com.example.focuskey.data

import java.util.Date

data class Session(
    val id: Long = System.currentTimeMillis(),
    val startDate: String,
    val startTime: String,
    val durationMinutes: Int,
    val tag: String,
    val status: String
)