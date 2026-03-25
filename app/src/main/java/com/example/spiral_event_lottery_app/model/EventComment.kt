package com.example.spiral_event_lottery_app.model

import com.google.firebase.Timestamp

data class EventComment @JvmOverloads constructor(
    var id: String = "",
    var eventId: String = "",
    var authorId: String = "",
    var authorName: String = "",
    var text: String = "",
    var role: String = "Entrant",
    var createdAt: Timestamp? = null
)