package com.example.spiral_event_lottery_app.data

/**
 * This helper encapsulates the logic required to notify a user when they have been
 * assigned as a co-organizer for a specific event. It formats and dispatches the
 * notification using the NotificationManager.
 *
 * Used by: CoOrganizerRepository, AssignCoOrganizerDialog
 * User Stories:
 * - US 01.09.01: As an entrant, I want to receive a notification if I have been invited to be a co-organizer for an event.
 */

object CoOrganizerNotificationHelper {
    /**
     * Sends a co-organizer invitation notification to the specified user.
     *
     * @param targetUserId The device ID of the user receiving the invitation
     * @param eventId The ID of the event for which the user is invited as a co-organizer
     * @param eventName The name of the event to display in the notification
     */
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