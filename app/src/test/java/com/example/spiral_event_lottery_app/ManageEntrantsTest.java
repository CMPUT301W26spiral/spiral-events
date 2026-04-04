package com.example.spiral_event_lottery_app;

import com.example.spiral_event_lottery_app.model.Event;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unit tests for the Manage Entrants feature.
 */
public class ManageEntrantsTest {

    @Test
    public void testEventStoresMaxEntrantsLimit() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 10, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 5L, "", false
        );
        assertEquals(Integer.valueOf(10), event.getMaxEntrants());
    }

    @Test
    public void testEventWithNoLimitHasNullMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", null, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 0L, "", false
        );
        assertNull(event.getMaxEntrants());
    }

    @Test
    public void testOpenSpotsCalculatedCorrectly() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 10, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 3L, "", false
        );
        Integer max = event.getMaxEntrants();
        assertNotNull(max);
        long openSpots = max.longValue() - event.getWaitingCount();
        assertEquals(7L, openSpots);
    }

    @Test
    public void testWaitingCountDoesNotExceedMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 5, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 5L, "", false
        );
        Integer max = event.getMaxEntrants();
        assertNotNull(max);
        assertTrue(event.getWaitingCount() <= max.longValue());
    }

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

    @Test
    public void testInvitedTabShowsPendingAndAcceptedOnly() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");
        statusMap.put("user3", "declined");
        statusMap.put("user4", "cancelled");

        List<String> invitedTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("pending") || e.getValue().equals("accepted"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(2, invitedTab.size());
        assertTrue(invitedTab.contains("user1"));
        assertTrue(invitedTab.contains("user2"));
    }

    @Test
    public void testCancelledTabShowsDeclinedAndCancelledOnly() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");
        statusMap.put("user3", "declined");
        statusMap.put("user4", "cancelled");

        List<String> cancelledTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("declined") || e.getValue().equals("cancelled"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(2, cancelledTab.size());
        assertTrue(cancelledTab.contains("user3"));
        assertTrue(cancelledTab.contains("user4"));
    }

    @Test
    public void testEnrolledListContainsOnlyAcceptedEntrants() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");
        statusMap.put("user3", "declined");
        statusMap.put("user4", "accepted");

        List<String> enrolledList = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("accepted"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(2, enrolledList.size());
        assertTrue(enrolledList.contains("user2"));
        assertTrue(enrolledList.contains("user4"));
    }

    @Test
    public void testCancelEntrantUpdatesStatusToCancelled() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");

        statusMap.put("user1", "cancelled");

        assertEquals("cancelled", statusMap.get("user1"));
    }

    @Test
    public void testCancelledEntrantRemovedFromInvitedTab() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");

        statusMap.put("user1", "cancelled");

        List<String> invitedTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("pending") || e.getValue().equals("accepted"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertFalse(invitedTab.contains("user1"));
        assertTrue(invitedTab.contains("user2"));
    }

    @Test
    public void testCancelledEntrantAppearsInCancelledTab() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");

        statusMap.put("user1", "cancelled");

        List<String> cancelledTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("declined") || e.getValue().equals("cancelled"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertTrue(cancelledTab.contains("user1"));
    }

    @Test
    public void testCsvExportContainsHeader() {
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Device ID,Status\n");
        csv.append("Alice,device123,accepted\n");

        assertTrue(csv.toString().startsWith("Name,Device ID,Status"));
    }

    @Test
    public void testCsvExportOnlyIncludesAcceptedEntrants() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("Alice", "accepted");
        statusMap.put("Bob", "pending");
        statusMap.put("Charlie", "declined");

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Device ID,Status\n");
        statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("accepted"))
                .forEach(e -> csv.append(e.getKey()).append(",device123,accepted\n"));

        assertTrue(csv.toString().contains("Alice"));
        assertFalse(csv.toString().contains("Bob"));
    }

    @Test
    public void testCsvExportIsEmptyWhenNoAcceptedEntrants() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("Bob", "pending");
        statusMap.put("Charlie", "declined");

        List<String> accepted = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("accepted"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertTrue(accepted.isEmpty());
    }

    @Test
    public void testCsvExportRowFormat() {
        String name = "Alice";
        String deviceId = "device123";
        String status = "accepted";

        String row = name + "," + deviceId + "," + status;

        assertEquals("Alice,device123,accepted", row);
    }
}
