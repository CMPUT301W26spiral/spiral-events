package com.example.spiral_event_lottery_app.data

object CoOrganizerNotificationHelper {

    fun sendInvite(
        targetUserId: String,
        eventId: String,
        eventName: String
    ) {
        NotificationManager.sendNotification(
            targetUserId,
            "Co-Organizer Invitation",
            "You have been invited as a co-organizer for $eventName",
            "CO_ORGANIZER_INVITE",
            eventName,
            eventId
        )
    }
}