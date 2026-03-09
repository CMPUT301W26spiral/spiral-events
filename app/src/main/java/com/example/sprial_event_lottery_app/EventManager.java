package com.example.sprial_event_lottery_app;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to manage a list of events locally.
 * This will later be replaced with/integrated into Firebase.
 */
public class EventManager {
    private static EventManager instance;
    private final List<Event> eventList;

    private EventManager() {
        eventList = new ArrayList<>();
    }

    public static synchronized EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void addEvent(Event event) {
        eventList.add(event);
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventList);
    }
}
