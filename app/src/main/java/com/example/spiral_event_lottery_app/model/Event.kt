package com.example.spiral_event_lottery_app.model

/**
 * Represents an event that entrants can view and join
 * Contains core information needed to display events on the UI
 * Each event corresponds to a document in the Firebase Firestore
 */
data class Event(
    val id: String = "",
    val name: String = "",
    val locationName: String = "",
    val timeText: String = "",
    var waitingCount: Long = 0
)