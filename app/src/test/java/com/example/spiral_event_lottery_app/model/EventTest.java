package com.example.spiral_event_lottery_app.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for the Event model class.
 */
public class EventTest {

    @Test
    public void testEventCreation() {
        Event event = new Event("1", "Gala", "Ballroom", "7:00 PM", 10L);
        
        assertEquals("1", event.getId());
        assertEquals("Gala", event.getName());
        assertEquals("Ballroom", event.getLocationName());
        assertEquals("7:00 PM", event.getTimeText());
        assertEquals(10L, event.getWaitingCount());
    }

    @Test
    public void testSetWaitingCount() {
        Event event = new Event();
        event.setWaitingCount(50L);
        assertEquals(50L, event.getWaitingCount());
    }
}
