package com.example.spiral_event_lottery_app.model

import java.io.Serializable

/**
 * Represents an event that entrants can view and join.
 *
 * Updated to support creation, management, and identification of organizers.
 */
data class Event @JvmOverloads constructor(
    var id: String = "",
    var name: String = "",
    var locationName: String = "",
    var interests: String = "",
    var description: String = "",
    var geolocation: String = "",
    var maxEntrants: Int? = null,

    // Date and Time fields for event and lottery draw
    var eventDate: String = "",
    var eventStartTime: String = "",
    var eventEndTime: String = "",
    var drawDate: String = "",
    var drawStartTime: String = "",
    var drawEndTime: String = "",
    var posterUriString: String? = null,

    var timeText: String = "",
    var waitingCount: Long = 0,
    var organizerId: String = ""
) : Serializable
