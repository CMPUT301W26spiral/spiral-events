package com.example.spiral_event_lottery_app.model

data class Event(
    val id: String = "",
    val name: String = "",
    val locationName: String = "",
    val timeText: String = "",
    var waitingCount: Long = 0
)