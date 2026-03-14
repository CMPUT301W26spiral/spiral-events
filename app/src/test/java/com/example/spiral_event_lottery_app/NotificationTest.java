package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import com.example.spiral_event_lottery_app.model.Notification;

/**
 * Comprehensive Unit tests for the Notification model class.
 * Covers construction, edge cases, and data integrity.
 */
public class NotificationTest {

    @Test
    public void testNotificationFullConstruction() {
        String title = "Accepted";
        String message = "You won!";
        String type = "ACCEPTED";
        String recipientId = "device_123";
        String eventName = "Soccer Match";
        String eventId = "event_999";

        Notification notification = new Notification(title, message, type, recipientId, eventName, eventId);

        assertEquals("Title should match", title, notification.getTitle());
        assertEquals("Message should match", message, notification.getMessage());
        assertEquals("Type should match", type, notification.getType());
        assertEquals("Recipient should match", recipientId, notification.getRecipientId());
        assertEquals("Event Name should match", eventName, notification.getEventName());
        assertEquals("Event ID should match", eventId, notification.getEventId());
    }

    @Test
    public void testDefaultConstructor() {
        // Firebase requires an empty constructor
        Notification notification = new Notification();
        assertNotNull("Object should be created", notification);
        assertNull("Fields should be null by default", notification.getTitle());
    }

    @Test
    public void testFormattedDateNull() {
        Notification notification = new Notification();
        // Method should handle null timestamps gracefully
        assertEquals("Should return 'Just now' for null timestamp", "Just now", notification.getFormattedDate());
    }

    @Test
    public void testMissingEventId() {
        // Edge case: A notification without an eventId (e.g. system alert)
        Notification notification = new Notification("Alert", "System update", "ORGANIZER", "user1", null, null);
        assertNull("Event ID should be null", notification.getEventId());
        assertNull("Event Name should be null", notification.getEventName());
    }
}
