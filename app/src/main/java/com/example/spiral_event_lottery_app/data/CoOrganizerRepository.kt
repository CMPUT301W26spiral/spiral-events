package com.example.spiral_event_lottery_app.data

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

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