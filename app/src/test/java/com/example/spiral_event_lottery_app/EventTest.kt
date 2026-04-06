package com.example.spiral_event_lottery_app

import com.example.spiral_event_lottery_app.model.Event
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the Event model class.
 */
class EventTest {

    /**
     * Verifies that an Event object is correctly initialized with provided values.
     */
    @Test
    fun testEventCreation() {
        val event = Event(
            id = "test_id",
            name = "Hockey Night",
            locationName = "Edmonton",
            waitingCount = 5L,
            organizerId = "org_123"
        )

        assertEquals("test_id", event.id)
        assertEquals("Hockey Night", event.name)
        assertEquals("Edmonton", event.locationName)
        assertEquals(5L, event.waitingCount)
        assertEquals("org_123", event.organizerId)
    }

    /**
     * Verifies that Event properties can be updated correctly.
     */
    @Test
    fun testEventUpdate() {
        val event = Event()
        event.name = "New Name"
        event.waitingCount = 10L
        
        assertEquals("New Name", event.name)
        assertEquals(10L, event.waitingCount)
    }
}
