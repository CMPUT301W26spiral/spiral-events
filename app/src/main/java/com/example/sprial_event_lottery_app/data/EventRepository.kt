package com.example.spiral_event_lottery_app.data

import com.example.spiral_event_lottery_app.model.Event

class EventRepository {

    fun listenToOpenEvents(
        onUpdate: (List<Event>) -> Unit,
        onError: (Exception) -> Unit
    ) {

        onUpdate(
            listOf(
                Event(
                    id = "event1",
                    name = "Family Swimming Lessons",
                    locationName = "Peter Hemingway Aquatic Centre",
                    timeText = "Sat, Mar. 4, 2026 10:00–11:00 AM",
                    waitingCount = 71
                ),
                Event(
                    id = "event2",
                    name = "Beginner Piano Lessons",
                    locationName = "Garneau Community Centre",
                    timeText = "Sun, Mar. 5, 2026 2:00–3:00 PM",
                    waitingCount = 24
                )
            )
        )
    }
}