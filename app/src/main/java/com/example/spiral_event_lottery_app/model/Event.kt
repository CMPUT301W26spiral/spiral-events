package com.example.spiral_event_lottery_app.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Represents an event that entrants can view and join.
 *
 * Support creation, management, and identification of organizers.
 */
data class Event @JvmOverloads constructor(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("locationName") @set:PropertyName("locationName") var locationName: String = "",
    @get:PropertyName("interests") @set:PropertyName("interests") var interests: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("geolocation") @set:PropertyName("geolocation") var geolocation: String = "",
    @get:PropertyName("maxEntrants") @set:PropertyName("maxEntrants") var maxEntrants: Int? = null,

    // Date and Time fields for event and lottery draw
    @get:PropertyName("eventDate") @set:PropertyName("eventDate") var eventDate: String = "",
    @get:PropertyName("eventStartTime") @set:PropertyName("eventStartTime") var eventStartTime: String = "",
    @get:PropertyName("eventEndTime") @set:PropertyName("eventEndTime") var eventEndTime: String = "",
    @get:PropertyName("drawDate") @set:PropertyName("drawDate") var drawDate: String = "",
    @get:PropertyName("drawStartTime") @set:PropertyName("drawStartTime") var drawStartTime: String = "",
    @get:PropertyName("drawEndTime") @set:PropertyName("drawEndTime") var drawEndTime: String = "",
    @get:PropertyName("posterUriString") @set:PropertyName("posterUriString") var posterUriString: String? = null,

    @get:PropertyName("timeText") @set:PropertyName("timeText") var timeText: String = "",
    @get:PropertyName("waitingCount") @set:PropertyName("waitingCount") var waitingCount: Long = 0,
    @get:PropertyName("organizerId") @set:PropertyName("organizerId") var organizerId: String = "",
    @get:PropertyName("qrCodeUrl") @set:PropertyName("qrCodeUrl") var qrCodeUrl: String? = null,
    @get:PropertyName("qrHash") @set:PropertyName("qrHash") var qrHash: String? = null
) : Serializable
