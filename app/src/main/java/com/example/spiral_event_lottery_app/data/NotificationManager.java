package com.example.spiral_event_lottery_app.data;

import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class to manage the sending of notifications to Firebase Firestore.
 * Provides static methods to create notification documents that are monitored by the app.
 * Now respects user notification preferences stored in their profile.
 */
public class NotificationManager {
    private static final String COLLECTION_NAME = "notifications";

    /**
     * Sends a notification to a specific user by saving it to Firestore, 
     * but only if the user has enabled that specific type of notification in their profile.
     *
     * @param recipientId The device ID of the user (Entrant) receiving the notification.
     * @param title       The header title of the notification.
     * @param message     The body text of the notification.
     * @param type        The category: "ACCEPTED", "DENIED", "ORGANIZER", or "REQUESTED".
     * @param eventName   The name of the event.
     * @param eventId     The unique ID of the event for navigation.
     */
    public static void sendNotification(String recipientId, String title, String message, String type, String eventName, String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch the user's notification preferences from their profile
        db.collection("users").document(recipientId).get().addOnSuccessListener(documentSnapshot -> {
            boolean shouldSend = true;

            if (documentSnapshot.exists()) {
                // Map the notification 'type' to the user's preference fields
                switch (type) {
                    case "ACCEPTED":
                        // US 01.04.01 - Receive notification when chosen
                        shouldSend = documentSnapshot.getBoolean("notifyWhenChosen") != null ? 
                                     documentSnapshot.getBoolean("notifyWhenChosen") : true;
                        break;
                    case "DENIED":
                        // US 01.04.02 - Receive notification when not chosen
                        shouldSend = documentSnapshot.getBoolean("notifyWhenNotChosen") != null ? 
                                     documentSnapshot.getBoolean("notifyWhenNotChosen") : true;
                        break;
                    case "ORGANIZER":
                        // US 01.04.03 - Opt out of receiving notifications from organizers
                        shouldSend = documentSnapshot.getBoolean("notifyOrganizersAdmins") != null ? 
                                     documentSnapshot.getBoolean("notifyOrganizersAdmins") : true;
                        break;
                    case "REQUESTED":
                        // Confirmation of joining - usually always sent unless otherwise specified
                        shouldSend = true;
                        break;
                }
            }

            // 2. Only add the notification to Firestore if the user hasn't "muted" it
            if (shouldSend) {
                Notification notification = new Notification(title, message, type, recipientId, eventName, eventId);
                db.collection(COLLECTION_NAME).add(notification);
            }
        });
    }
}
