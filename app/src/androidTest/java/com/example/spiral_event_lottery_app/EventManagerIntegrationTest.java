package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.spiral_event_lottery_app.ui.event_creation.EventManager;
import com.example.spiral_event_lottery_app.model.Event;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Instrumented test for EventManager.
 * Runs on a device/emulator to allow Firebase initialization logic to execute.
 */
@RunWith(AndroidJUnit4.class)
public class EventManagerIntegrationTest {

    @Test
    public void testSingleton() {
        // Runs on device, so Firebase initialization in constructor won't crash
        EventManager instance1 = EventManager.getInstance();
        EventManager instance2 = EventManager.getInstance();
        assertEquals("Both instances should be the same", instance1, instance2);
    }

    @Test
    public void testAddEventLocal() {
        EventManager manager = EventManager.getInstance();
        int initialSize = manager.getEvents().size();
        
        Event event = new Event();
        event.setName("Integration Test Event");
        manager.addEvent(event);
        
        List<Event> events = manager.getEvents();
        assertEquals("Size should increase by 1", initialSize + 1, events.size());
        
        boolean found = false;
        for (Event e : events) {
            if ("Integration Test Event".equals(e.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("Event should be found in the list", found);
        assertNotNull("Event ID should be generated", event.getId());
    }
}
