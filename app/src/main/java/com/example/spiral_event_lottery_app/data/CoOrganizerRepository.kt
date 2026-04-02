package com.example.spiral_event_lottery_app.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**

 * This class is responsible for assigning entrants as co-organizers for a given event.
 * It ensures that only the main organizer can assign co-organizers, prevents duplicate assignments,
 * and removes the user from the waitlist if they are promoted.

 * Used by: AssignCoOrganizerDialog
 * User Stories:
 * - US 02.09.01: As an organizer, I want to assign an entrant as a co-organizer for my event,
 *   which prevents them from joining the entrant pool for that event.
 */

class CoOrganizerRepository(context: Context) {

    interface SuccessCallback {
        fun onSuccess()
    }

    interface ErrorCallback {
        fun onError(e: Exception)
    }

    private val db = FirebaseFirestore.getInstance()
    private val deviceId = DeviceIdProvider.getDeviceId(context)

    private fun coOrgRef(eventId: String) =
        db.collection("events").document(eventId).collection("co_organizers")

    /**
     * Assigns a user as a co-organizer for a specific event.
     *
     * This method:
     * - Verifies that the current user is the main organizer of the event
     * - Ensures the target user is not already a co-organizer
     * - Removes the user from the waitlist if they are currently on it
     * - Updates the waiting list count accordingly
     * - Adds the user to the co_organizers subcollection with a timestamp
     */
    fun assignCoOrganizer(
        eventId: String,
        targetUserId: String,
        onSuccess: SuccessCallback,
        onError: ErrorCallback
    ) {
        val eventRef = db.collection("events").document(eventId)
        val coOrgDoc = coOrgRef(eventId).document(targetUserId)
        val waitlistRef = eventRef.collection("waitlist").document(targetUserId)

        db.runTransaction { transaction ->
            val eventDoc = transaction.get(eventRef)
            val organizerId = eventDoc.getString("organizerId") ?: ""

            if (organizerId != deviceId) {
                throw Exception("ONLY_ORGANIZER")
            }

            val coOrgCheck = transaction.get(coOrgDoc)
            if (coOrgCheck.exists()) {
                throw Exception("ALREADY_CO_ORGANIZER")
            }

            val waitlistDoc = transaction.get(waitlistRef)
            if (waitlistDoc.exists()) {
                transaction.delete(waitlistRef)

                val currentCount = eventDoc.getLong("waiting_count") ?: 0L
                transaction.update(eventRef, "waiting_count", (currentCount - 1).coerceAtLeast(0))
            }

            val data = hashMapOf(
                "device_id" to targetUserId,
                "assigned_at" to Timestamp.now()
            )

            transaction.set(coOrgDoc, data)
            null
        }.addOnSuccessListener {
            onSuccess.onSuccess()
        }.addOnFailureListener { e ->
            onError.onError(e)
        }
    }
}