package com.example.spiral_event_lottery_app.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Data class for tags to support AI categorization.
 * Includes default values and property names for Firebase compatibility.
 */
data class Tag @JvmOverloads constructor(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("parents") @set:PropertyName("parents") var parents: List<String> = emptyList(),
    @get:PropertyName("synonyms") @set:PropertyName("synonyms") var synonyms: List<String> = emptyList(),
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "pending"
) : Serializable
