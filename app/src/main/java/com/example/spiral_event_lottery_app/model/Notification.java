package com.example.spiral_event_lottery_app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Model class representing a notification in the spiral event lottery app.
 * Stores information about event updates sent to users.
 */
public class Notification {
    @DocumentId
    private String id;
    private String title;
    private String message;
    private String type;
    private String recipientId;
    private String eventName;
    private String eventId; 
    @ServerTimestamp
    private Timestamp timestamp;

    /**
     * Default constructor required for Firebase Firestore deserialization.
     */
    public Notification() {}

    /**
     * Constructs a new Notification with the specified details.
     *
     * @param title       The title of the notification (e.g., "Accepted", "Denied").
     * @param message     The descriptive message of the notification.
     * @param type        The type of notification determining UI styling (e.g., "ACCEPTED").
     * @param recipientId The device ID of the user receiving the notification.
     * @param eventName   The name of the event associated with this notification.
     * @param eventId     The unique ID of the event for navigation purposes.
     */
    public Notification(String title, String message, String type, String recipientId, String eventName, String eventId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.recipientId = recipientId;
        this.eventName = eventName;
        this.eventId = eventId;
    }

    /** @return The unique Firestore document ID. */
    public String getId() { return id; }
    /** @return The title of the notification. */
    public String getTitle() { return title; }
    /** @return The notification message. */
    public String getMessage() { return message; }
    /** @return The type of notification (e.g., "ACCEPTED", "DENIED"). */
    public String getType() { return type; }
    /** @return The device ID of the recipient. */
    public String getRecipientId() { return recipientId; }
    /** @return The name of the related event. */
    public String getEventName() { return eventName; }
    /** @return The ID of the related event. */
    public String getEventId() { return eventId; }
    /** @return The timestamp when the notification was created on the server. */
    public Timestamp getTimestamp() { return timestamp; }
    
    /**
     * Returns a human-readable string representation of the notification timestamp.
     *
     * @return Formatted date string (e.g., "MMM dd, yyyy h:mm a") or "Just now" if null.
     */
    @Exclude
    public String getFormattedDate() {
        if (timestamp == null) return "Just now";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}
