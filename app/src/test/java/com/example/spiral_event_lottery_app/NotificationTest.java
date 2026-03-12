package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import com.example.spiral_event_lottery_app.model.Notification;

/**
 * Unit tests for the Notification model class including edge cases.
 */
public class NotificationTest {

    @Test
    public void testNotificationConstruction() {
        String title = "Accepted";
        String message = "You won!";
        String type = "ACCEPTED";
        String recipientId = "test_user_123";
        String eventName = "Hockey Night";
        String eventId = "event_001";

        Notification notification = new Notification(title, message, type, recipientId, eventName, eventId);

        assertEquals(title, notification.getTitle());
        assertEquals(message, notification.getMessage());
        assertEquals(type, notification.getType());
        assertEquals(recipientId, notification.getRecipientId());
        assertEquals(eventName, notification.getEventName());
        assertEquals(eventId, notification.getEventId());
    }

    @Test
    public void testDefaultConstructor() {
        Notification notification = new Notification();
        assertNotNull(notification);
        assertNull(notification.getTitle()); // Edge case: ensure blank fields are null, not crashing
    }

    @Test
    public void testFormattedDateNull() {
        Notification notification = new Notification();
        // Should return "Just now" if the Firebase timestamp hasn't been set yet
        assertEquals("Just now", notification.getFormattedDate());
    }

    @Test
    public void testMissingEventName() {
        // Edge case: Construction without an event name
        Notification notification = new Notification("Title", "Msg", "ACCEPTED", "user1", null, "id1");
        assertNull(notification.getEventName());
    }
}
