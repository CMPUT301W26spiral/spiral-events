package com.example.spiral_event_lottery_app.data

import android.content.Context
import com.example.spiral_event_lottery_app.model.EventComment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class EventCommentRepository(context: Context) {

    interface CommentsCallback {
        fun onUpdate(comments: List<EventComment>)
    }

    interface BooleanCallback {
        fun onResult(value: Boolean)
    }

    interface SuccessCallback {
        fun onSuccess()
    }

    interface ErrorCallback {
        fun onError(e: Exception)
    }

    private val db = FirebaseFirestore.getInstance()
    private val deviceId = DeviceIdProvider.getDeviceId(context)

    private fun commentsRef(eventId: String) =
        db.collection("events").document(eventId).collection("comments")

    fun listenToComments(
        eventId: String,
        onUpdate: CommentsCallback,
        onError: ErrorCallback
    ): ListenerRegistration {
        return commentsRef(eventId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError.onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    onUpdate.onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val comments = snapshot.documents.map { doc ->
                    EventComment(
                        id = doc.getString("id") ?: doc.id,
                        eventId = doc.getString("eventId") ?: eventId,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "User",
                        text = doc.getString("text") ?: "",
                        role = doc.getString("role") ?: "Entrant",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }

                onUpdate.onUpdate(comments)
            }
    }

    fun canManageComments(
        eventId: String,
        onResult: BooleanCallback,
        onError: ErrorCallback
    ) {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                val organizerId = doc.getString("organizerId") ?: ""
                onResult.onResult(organizerId == deviceId)
            }
            .addOnFailureListener { e ->
                onError.onError(Exception(e.message ?: "Failed to check organizer access"))
            }
    }

    fun addComment(
        eventId: String,
        rawText: String,
        onSuccess: SuccessCallback,
        onError: ErrorCallback
    ) {
        val text = rawText.trim()
        if (text.isEmpty()) {
            onError.onError(Exception("Comment cannot be empty"))
            return
        }

        val eventRef = db.collection("events").document(eventId)
        val userRef = db.collection("users").document(deviceId)

        eventRef.get()
            .addOnSuccessListener { eventDoc ->
                if (!eventDoc.exists()) {
                    onError.onError(Exception("Event not found"))
                    return@addOnSuccessListener
                }

                val organizerId = eventDoc.getString("organizerId") ?: ""

                userRef.get()
                    .addOnSuccessListener { userDoc ->
                        val authorName = userDoc.getString("name")?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: "User"
                        val role = if (deviceId == organizerId) "Organizer" else "Entrant"
                        val commentDoc = commentsRef(eventId).document()

                        val data = hashMapOf(
                            "id" to commentDoc.id,
                            "eventId" to eventId,
                            "authorId" to deviceId,
                            "authorName" to authorName,
                            "text" to text,
                            "role" to role,
                            "createdAt" to Timestamp.now()
                        )

                        commentDoc.set(data)
                            .addOnSuccessListener { onSuccess.onSuccess() }
                            .addOnFailureListener { e ->
                                onError.onError(Exception(e.message ?: "Failed to post comment"))
                            }
                    }
                    .addOnFailureListener { e ->
                        onError.onError(Exception(e.message ?: "Failed to load user"))
                    }
            }
            .addOnFailureListener { e ->
                onError.onError(Exception(e.message ?: "Failed to load event"))
            }
    }

    fun deleteComment(
        eventId: String,
        commentId: String,
        onSuccess: SuccessCallback,
        onError: ErrorCallback
    ) {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { eventDoc ->
                val organizerId = eventDoc.getString("organizerId") ?: ""
                if (organizerId != deviceId) {
                    onError.onError(Exception("Only the organizer can delete comments"))
                    return@addOnSuccessListener
                }

                commentsRef(eventId).document(commentId)
                    .delete()
                    .addOnSuccessListener { onSuccess.onSuccess() }
                    .addOnFailureListener { e ->
                        onError.onError(Exception(e.message ?: "Failed to delete comment"))
                    }
            }
            .addOnFailureListener { e ->
                onError.onError(Exception(e.message ?: "Failed to verify organizer"))
            }
    }
}