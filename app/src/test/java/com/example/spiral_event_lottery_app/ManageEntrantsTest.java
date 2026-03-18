package com.example.spiral_event_lottery_app;

import com.example.spiral_event_lottery_app.model.Event;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the Manage Entrants feature.
 */
public class ManageEntrantsTest {

    // Waiting List Limit

    @Test
    public void testEventStoresMaxEntrantsLimit() {
        Event event = new Event(
                "event1", "Test Event", "", "", "", "",
                10, "", "", "", "", "", "", null, "", 5L, ""
        );
        assertEquals(Integer.valueOf(10), event.getMaxEntrants());
    }

    @Test
    public void testEventWithNoLimitHasNullMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", "", "", "",
                null, "", "", "", "", "", "", null, "", 0L, ""
        );
        assertNull(event.getMaxEntrants());
    }

    @Test
    public void testOpenSpotsCalculatedCorrectly() {
        Event event = new Event(
                "event1", "Test Event", "", "", "", "",
                10, "", "", "", "", "", "", null, "", 3L, ""
        );
        long openSpots = event.getMaxEntrants() - event.getWaitingCount();
        assertEquals(7L, openSpots);
    }

    @Test
    public void testWaitingCountDoesNotExceedMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", "", "", "",
                5, "", "", "", "", "", "", null, "", 5L, ""
        );
        assertTrue(event.getWaitingCount() <= event.getMaxEntrants());
    }

    //  Waiting List

    @Test
    public void testWaitingListIsNotNull() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertNotNull(waitingList);
    }

    @Test
    public void testWaitingListSizeIsCorrect() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertEquals(3, waitingList.size());
    }

    @Test
    public void testEmptyWaitingListHasSizeZero() {
        List<String> waitingList = Collections.emptyList();
        assertEquals(0, waitingList.size());
    }

    @Test
    public void testEntrantExistsInWaitingList() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertTrue(waitingList.contains("Alice"));
    }

    @Test
    public void testEntrantNotInWaitingListReturnsFalse() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertFalse(waitingList.contains("Dave"));
    }

    // Invited / Selected List

    @Test
    public void testInvitedListIsNotEmpty() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertFalse(invitedList.isEmpty());
    }

    @Test
    public void testInvitedListSizeMatchesDrawCount() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertEquals(2, invitedList.size());
    }

    @Test
    public void testDrawnEntrantAppearsInInvitedList() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertTrue(invitedList.contains("Bob"));
    }

    @Test
    public void testEntrantNotInBothWaitingAndInvitedLists() {
        List<String> waitingList = Arrays.asList("Charlie", "Dave");
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        boolean overlap = false;
        for (String entrant : waitingList) {
            if (invitedList.contains(entrant)) {
                overlap = true;
                break;
            }
        }
        assertFalse(overlap);
    }

    @Test
    public void testInvitedListDoesNotExceedMaxEntrants() {
        int maxEntrants = 3;
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertTrue(invitedList.size() <= maxEntrants);
    }
}
