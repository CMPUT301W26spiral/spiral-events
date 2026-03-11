package com.example.spiral_event_lottery_app.data;

import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationManager {
    private static final String COLLECTION_NAME = "notifications";

    /**
     * Sends a notification to a specific user by saving it to Firestore.
     * @param recipientId The ID of the user (Entrant) receiving the notification.
     * @param title The title (e.g., "Accepted", "Denied").
     * @param message The content of the notification.
     * @param type The type (e.g., "ACCEPTED", "DENIED", "REQUESTED").
     * @param eventName The name of the event this notification is about.
     */
    public static void sendNotification(String recipientId, String title, String message, String type, String eventName) {
        Notification notification = new Notification(title, message, type, recipientId, eventName);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_NAME)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Notification sent successfully
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }
}
