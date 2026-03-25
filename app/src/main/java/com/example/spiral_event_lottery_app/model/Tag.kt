package com.example.spiral_event_lottery_app.model

import java.io.Serializable

data class Tag(
    val id: String = "",
    val parents: List<String> = emptyList(),
    val synonyms: List<String> = emptyList()
) : Serializable
