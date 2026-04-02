package com.example.spiral_event_lottery_app.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class User @JvmOverloads constructor(
    var deviceId: String = "",
    var name: String = "",
    var email: String = "",
    @get:PropertyName("phone_number")
    @set:PropertyName("phone_number")
    var phoneNumber: String? = "",
    var photoUrl: String? = null,
    var isAdmin: Boolean = false,
    var eventList: MutableList<String> = mutableListOf(), // store strings of eventId instead of actual event item
    
    var interested: List<String> = emptyList(),
    var notInterested: List<String> = emptyList(),
    var customInterests: List<String> = emptyList()
) : Serializable
