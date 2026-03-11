package com.example.spiral_event_lottery_app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Notification {
    private String title;
    private String message;
    private String type;
    private String recipientId;
    private String eventName;
    @ServerTimestamp
    private Timestamp timestamp;

    public Notification() {}

    public Notification(String title, String message, String type, String recipientId, String eventName) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.recipientId = recipientId;
        this.eventName = eventName;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getRecipientId() { return recipientId; }
    public String getEventName() { return eventName; }
    public Timestamp getTimestamp() { return timestamp; }
    
    @Exclude
    public String getFormattedDate() {
        if (timestamp == null) return "Just now";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}
