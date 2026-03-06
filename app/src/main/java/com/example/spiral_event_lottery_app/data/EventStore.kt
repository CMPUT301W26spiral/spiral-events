package com.example.spiral_event_lottery_app.data

import com.example.spiral_event_lottery_app.model.Event

object EventStore {

    val allEvents: MutableList<Event> = mutableListOf(
        Event(
            id = "e1",
            name = "Family Swimming Lessons",
            locationName = "Peter Hemingway Aquatic Centre",
            timeText = "Sat, Mar. 4, 2026 10:00–11:00 AM",
            waitingCount = 71
        ),
        Event(
            id = "e2",
            name = "Beginner Piano Lessons",
            locationName = "Garneau Community Centre",
            timeText = "Sun, Mar. 5, 2026 2:00–3:00 PM",
            waitingCount = 24
        )
    )

    private val joinedEventIds: MutableSet<String> = mutableSetOf()

    fun isJoined(eventId: String): Boolean = joinedEventIds.contains(eventId)

    fun join(eventId: String) {
        if (!joinedEventIds.contains(eventId)) {
            joinedEventIds.add(eventId)
            allEvents.find { it.id == eventId }?.let { it.waitingCount += 1 }
        }
    }

    fun leave(eventId: String) {
        if (joinedEventIds.contains(eventId)) {
            joinedEventIds.remove(eventId)
            allEvents.find { it.id == eventId }?.let { event ->
                if (event.waitingCount > 0) event.waitingCount -= 1
            }
        }
    }

    fun joinedEvents(): List<Event> =
        allEvents.filter { joinedEventIds.contains(it.id) }
}