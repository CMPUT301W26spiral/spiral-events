package com.example.spiral_event_lottery_app.model

import java.io.Serializable

data class User @JvmOverloads constructor(
    var deviceId: String = "",
    var name: String = "",
    var email: String = "",
    var phoneNumber: String? = "",
    var photoUrl: String? = null,
    var isAdmin: Boolean = false,
    var eventList: MutableList<String> = mutableListOf() // store strings of eventId instead of actual event item

) : Serializable