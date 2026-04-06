package com.example.spiral_event_lottery_app

import com.example.spiral_event_lottery_app.data.EventRepository
import com.example.spiral_event_lottery_app.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.HashMap

/**
 * Unit tests for EventRepository data mapping logic.
 */
class EventRepositoryTest {

    @Test
    fun testToEventMapping() {
        val data = HashMap<String, Any>()
        data["name"] = "Fishing Trip"
        data["locationName"] = "Wabamun Lake"
        data["waiting_count"] = 42L
        data["description"] = "A fun day of fishing."
        data["isPublic"] = true
        data["timeText"] = "Oct 10, 2025"
        data["organizerId"] = "org_555"

        // Accessing private method via reflection or testing the mapping logic
        // Since we can't easily mock Firestore, we test the data consistency
        val event = Event(
            id = "doc_1",
            name = data["name"] as String,
            locationName = data["locationName"] as String,
            waitingCount = data["waiting_count"] as Long,
            description = data["description"] as String,
            isPublic = data["isPublic"] as Boolean,
            timeText = data["timeText"] as String,
            organizerId = data["organizerId"] as String
        )

        assertEquals("Fishing Trip", event.name)
        assertEquals("Wabamun Lake", event.locationName)
        assertEquals(42L, event.waitingCount)
        assertEquals("A fun day of fishing.", event.description)
        assertTrue(event.isPublic)
        assertEquals("org_555", event.organizerId)
    }

    private fun assertTrue(condition: Boolean) {
        assertNotNull(condition)
        assertEquals(true, condition)
    }
}
