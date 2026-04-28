package com.example.spiral_event_lottery_app.data;

import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class to manage the sending of notifications to Firebase Firestore.
 * Provides static methods to create notification documents that are monitored by the app.
 * Now respects user notification preferences stored in Firestore.
 */
public class NotificationManager {
    private static final String COLLECTION_NAME = "notifications";

    /**
     * Sends a notification to a specific user by saving it to Firestore,
     * but only if the user has opted in to that specific type of notification.
     *
     * @param recipientId The device ID of the user (Entrant) receiving the notification.
     * @param title       The header title of the notification (e.g., "Accepted").
     * @param message     The body text of the notification.
     * @param type        The category of notification (e.g., "ACCEPTED", "DENIED", "ORGANIZER", "ADMIN").
     * @param eventName   The name of the event associated with the notification.
     * @param eventId     The unique ID of the event for navigation.
     */
    public static void sendNotification(String recipientId, String title, String message, String type, String eventName, String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Fetch recipient's preferences before sending
        db.collection("users").document(recipientId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                boolean shouldSend = true;
                
                if ("ACCEPTED".equals(type)) {
                    Boolean pref = documentSnapshot.getBoolean("notifyWhenChosen");
                    shouldSend = (pref == null || pref);
                } else if ("DENIED".equals(type)) {
                    Boolean pref = documentSnapshot.getBoolean("notifyWhenNotChosen");
                    shouldSend = (pref == null || pref);
                } else if ("ORGANIZER".equals(type) || "ADMIN".equals(type)) {
                    Boolean pref = documentSnapshot.getBoolean("notifyOrganizersAdmins");
                    shouldSend = (pref == null || pref);
                }
                
                if (shouldSend) {
                    performSend(recipientId, title, message, type, eventName, eventId);
                }
            } else {
                // If user doesn't exist or pref not set, default to sending
                performSend(recipientId, title, message, type, eventName, eventId);
            }
        });
    }

    /**
     * Internal method to actually add the notification document to Firestore.
     */
    private static void performSend(String recipientId, String title, String message, String type, String eventName, String eventId) {
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
