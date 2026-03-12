package com.example.spiral_event_lottery_app.data;

import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class to manage the sending of notifications to Firebase Firestore.
 * Provides static methods to create notification documents that are monitored by the app.
 */
public class NotificationManager {
    private static final String COLLECTION_NAME = "notifications";

    /**
     * Sends a notification to a specific user by saving it to Firestore.
     * This method is decoupled from the UI and can be called from anywhere in the app logic.
     *
     * @param recipientId The device ID of the user (Entrant) receiving the notification.
     * @param title       The header title of the notification (e.g., "Accepted").
     * @param message     The body text of the notification.
     * @param type        The category of notification (e.g., "ACCEPTED", "DENIED", "REQUESTED").
     * @param eventName   The name of the event associated with the notification.
     * @param eventId     The unique ID of the event for navigation.
     */
    public static void sendNotification(String recipientId, String title, String message, String type, String eventName, String eventId) {
        Notification notification = new Notification(title, message, type, recipientId, eventName, eventId);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_NAME)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Notification sent successfully to Firestore
                })
                .addOnFailureListener(e -> {
                    // Potential logging for failed network requests
                });
    }
}
