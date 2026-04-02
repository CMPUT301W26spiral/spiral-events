package com.example.spiral_event_lottery_app.data

import android.content.Context
import com.example.spiral_event_lottery_app.model.EventComment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * This class is responsible for:
 * - Listening to real-time comment updates for an event
 * - Posting new comments by entrants or organizers
 * - Determining whether a user has permission to manage (delete) comments
 * - Deleting comments (organizer only)
 *
 *
 * Used by: EventDetailsFragment, Comment UI components
 *
 * User Stories:
 * - US 01.08.01: As an entrant, I want to post a comment on an event.
 * - US 01.08.02: As an entrant, I want to view comments on an event.
 * - US 02.08.01: As an organizer, I want to view and delete entrant comments on my event.
 * - US 02.08.02: As an organizer, I want to comment on my events.
 */

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

    /**
     * Returns a reference to the comments subcollection for a given event.
     *
     * @param eventId The ID of the event
     * @return Firestore collection reference for comments
     */
    private fun commentsRef(eventId: String) =
        db.collection("events").document(eventId).collection("comments")

    /**
     * Listens for real-time updates to comments for a specific event.
     *
     * Comments are ordered by creation time in ascending order.
     * Returns a ListenerRegistration that can be used to stop listening.
     *
     * @param eventId The ID of the event
     * @param onUpdate Callback triggered when comments are updated
     * @param onError Callback triggered if an error occurs
     * @return ListenerRegistration for managing the listener lifecycle
     */
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

    /**
     * Checks whether the current user has permission to manage (delete) comments.
     *
     * Only the event organizer is allowed to delete comments.
     *
     * @param eventId The ID of the event
     * @param onResult Callback returning true if user is organizer, false otherwise
     * @param onError Callback triggered if an error occurs
     */
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

    /**
     * Adds a new comment to the specified event.
     *
     * This method:
     * - Validates that the comment text is not empty
     * - Retrieves event and user information
     * - Determines the role of the commenter (Organizer or Entrant)
     * - Stores the comment in Firestore with metadata
     *
     * @param eventId The ID of the event
     * @param rawText The raw comment text entered by the user
     * @param onSuccess Callback triggered when the comment is successfully posted
     * @param onError Callback triggered if the operation fails
     */
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
    /**
     * Deletes a comment from the specified event.
     *
     * Only the event organizer is allowed to delete comments.
     *
     * @param eventId The ID of the event
     * @param commentId The ID of the comment to delete
     * @param onSuccess Callback triggered when deletion is successful
     * @param onError Callback triggered if the operation fails or user lacks permission
     */
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