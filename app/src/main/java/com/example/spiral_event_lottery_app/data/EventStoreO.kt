package com.example.spiral_event_lottery_app.data

import android.content.Context
import com.example.spiral_event_lottery_app.model.Event
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * EventStoreO handles data access for the organizer-side event functionality.
 * It is responsible for fetching events that were created by the current user (organizer).
 */
class EventStoreO(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Fetches all events from Firestore where the organizerId matches the current device hardware ID.
     * 
     * @param onComplete A callback function that receives the list of retrieved Event objects.
     */
    fun organizerEvents(onComplete: (List<Event>) -> Unit) {
        val currentDeviceId = DeviceIdProvider.getDeviceId(context)
        
        db.collection("events")
            .whereEqualTo("organizerId", currentDeviceId)
            .get()
            .addOnSuccessListener { snapshot ->
                val events = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    toEvent(doc.id, data)
                }
                onComplete(events)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    /**
     * Converts raw Firestore document data into a structured Event model object.
     * Maps fields based on the names used in the Event.kt data class.
     * 
     * @param documentId The ID of the document in Firestore.
     * @param data The map of fields retrieved from the document.
     * @return A populated Event object.
     */
    private fun toEvent(documentId: String, data: Map<String, Any>): Event {
        val name = data["name"] as? String ?: ""
        
        // Handle variations in field naming
        val location = data["locationName"] as? String 
            ?: data["location_name"] as? String 
            ?: data["location"] as? String ?: ""

        var timeText = data["timeText"] as? String ?: ""
        if (timeText.isEmpty()) {
            val startTime = data["event_start_time"] as? Timestamp
            val endTime = data["event_end_time"] as? Timestamp
            timeText = formatTimeRange(startTime, endTime)
        }

        val waitingCount = (data["waitingCount"] as? Long) 
            ?: (data["waiting_count"] as? Long) ?: 0L

        return Event(
            id = documentId,
            name = name,
            locationName = location,
            timeText = timeText,
            waitingCount = waitingCount,
            eventCreated = data["eventCreated"] as? String ?: "",
            organizerId = data["organizerId"] as? String ?: "",
            description = data["description"] as? String ?: "",
            posterUriString = data["posterUriString"] as? String ?: data["posterUrl"] as? String
        )
    }

    /**
     * Formats event start and end timestamps into a readable date-time range string.
     * 
     * @param start The starting Firebase timestamp.
     * @param end The ending Firebase timestamp.
     * @return A formatted string like "Mon, Oct 31, 2025 5:00 PM-7:00 PM".
     */
    private fun formatTimeRange(start: Timestamp?, end: Timestamp?): String {
        if (start == null || end == null) return "No Time"
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.CANADA)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.CANADA)
        val startDate = start.toDate()
        return "${dateFormat.format(startDate)} ${timeFormat.format(startDate)}-${timeFormat.format(end.toDate())}"
    }
}
