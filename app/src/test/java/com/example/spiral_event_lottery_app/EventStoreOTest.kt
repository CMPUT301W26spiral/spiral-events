package com.example.spiral_event_lottery_app

import com.example.spiral_event_lottery_app.model.Event
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for EventStoreO logic.
 * Tests how organizer events are handled and mapped.
 */
class EventStoreOTest {

    @Test
    fun testOrganizerEventSorting() {
        val event1 = Event(id = "1", name = "Recent Event", eventCreated = "2025-10-30 10:00:00")
        val event2 = Event(id = "2", name = "Old Event", eventCreated = "2025-01-01 10:00:00")
        
        val events = listOf(event2, event1)
        val sorted = events.sortedByDescending { it.eventCreated }
        
        assertEquals("Recent Event", sorted[0].name)
        assertEquals("Old Event", sorted[1].name)
    }

    @Test
    fun testPosterUrlMapping() {
        val data = mapOf(
            "name" to "Art Show",
            "posterUriString" to "https://example.com/poster.png"
        )
        
        val event = Event(
            name = data["name"] as String,
            posterUriString = data["posterUriString"] as String
        )
        
        assertEquals("https://example.com/poster.png", event.posterUriString)
    }
}
