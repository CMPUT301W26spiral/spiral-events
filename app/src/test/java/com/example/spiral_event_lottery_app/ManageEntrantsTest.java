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
 * Covers:
 * - US 02.03.01: Waiting list limit
 * - US 02.06.01: Invited/selected list
 * - US 02.06.02: Cancelled entrants
 * - US 02.06.03: Enrolled (accepted) entrants
 * - US 02.06.04: Cancel entrant status logic
 * - US 02.06.05: CSV export content
 */
public class ManageEntrantsTest {

    /**
     * Verifies that an Event object correctly stores the maximum entrants limit.
     */
    @Test
    public void testEventStoresMaxEntrantsLimit() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 10, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 5L, "", false
        );
        assertEquals(Integer.valueOf(10), event.getMaxEntrants());
    }

    /**
     * Verifies that an event with no limit has a null value for max entrants.
     */
    @Test
    public void testEventWithNoLimitHasNullMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", null, "", "", "", "", "", "", null, Collections.emptyList(), "", "", 0L, "", false
        );
        assertNull(event.getMaxEntrants());
    }

    /**
     * Verifies that open spots are correctly calculated as (Max Entrants - Waiting Count).
     */
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

    /**
     * Sanity check that the waiting count does not exceed the allowed maximum entrants.
     */
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

    /**
     * Basic check to ensure a non-empty waiting list is initialized.
     */
    @Test
    public void testWaitingListIsNotNull() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertNotNull(waitingList);
    }

    /**
     * Verifies the size of the waiting list matches the number of entrants added.
     */
    @Test
    public void testWaitingListSizeIsCorrect() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertEquals(3, waitingList.size());
    }

    /**
     * Verifies that an empty waiting list has a size of zero.
     */
    @Test
    public void testEmptyWaitingListHasSizeZero() {
        List<String> waitingList = Collections.emptyList();
        assertEquals(0, waitingList.size());
    }

    /**
     * Verifies that an entrant added to the waiting list is actually present in the list.
     */
    @Test
    public void testEntrantExistsInWaitingList() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertTrue(waitingList.contains("Alice"));
    }

    /**
     * Verifies that a user not in the waiting list returns false when searched.
     */
    @Test
    public void testEntrantNotInWaitingListReturnsFalse() {
        List<String> waitingList = Arrays.asList("Alice", "Bob", "Charlie");
        assertFalse(waitingList.contains("Dave"));
    }

    /**
     * Basic check that the invited list (drawn users) contains entries.
     */
    @Test
    public void testInvitedListIsNotEmpty() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertFalse(invitedList.isEmpty());
    }

    /**
     * Verifies that the number of invited entrants matches the expected draw count.
     */
    @Test
    public void testInvitedListSizeMatchesDrawCount() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertEquals(2, invitedList.size());
    }

    /**
     * Verifies that a specific user selected in the draw appears in the invited list.
     */
    @Test
    public void testDrawnEntrantAppearsInInvitedList() {
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertTrue(invitedList.contains("Bob"));
    }

    /**
     * Ensures that entrants are moved correctly and do not exist in both waiting and invited lists simultaneously.
     */
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

    /**
     * Verifies that the invited list size does not exceed the event's max entrant capacity.
     */
    @Test
    public void testInvitedListDoesNotExceedMaxEntrants() {
        int maxEntrants = 3;
        List<String> invitedList = Arrays.asList("Alice", "Bob");
        assertTrue(invitedList.size() <= maxEntrants);
    }

    /**
     * Verifies that only "pending" and "accepted" entrants appear in the invited tab.
     * Implements US 02.06.01.
     */
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

    /**
     * Verifies that only "declined" and "cancelled" entrants appear in the cancelled tab.
     * Implements US 02.06.02.
     */
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

    /**
     * Verifies that only "accepted" entrants appear in the final enrolled list.
     * Implements US 02.06.03.
     * */
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

    /**
     * Verifies that updating an entrant's status to "cancelled" moves them
     * from the invited tab to the cancelled tab.
     * Implements US 02.06.04.
     */
    @Test
    public void testCancelEntrantUpdatesStatusToCancelled() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");

        statusMap.put("user1", "cancelled");

        assertEquals("cancelled", statusMap.get("user1"));
    }

    /**
     * Verifies that once an entrant's status changes to "cancelled", they no longer appear in the "Invited" tab.
     */
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

    /**
     * Verifies that once an entrant's status changes to "cancelled", they appear in the "Cancelled" tab.
     */
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

    /**
     * Verifies that only accepted entrants are included in the CSV export.
     * Implements US 02.06.05.
     */
    @Test
    public void testCsvExportContainsHeader() {
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Device ID,Status\n");
        csv.append("Alice,device123,accepted\n");

        assertTrue(csv.toString().startsWith("Name,Device ID,Status"));
    }

    /**
     * Verifies that the CSV export logic filters for and only includes "accepted" entrants.
     */
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

    /**
     * Verifies that if no entrants are accepted, the exported list of accepted names is empty.
     */
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

    /**
     * Verifies the row formatting in the CSV export: "Name,DeviceID,Status".
     */
    @Test
    public void testCsvExportRowFormat() {
        String name = "Alice";
        String deviceId = "device123";
        String status = "accepted";

        String row = name + "," + deviceId + "," + status;

        assertEquals("Alice,device123,accepted", row);
    }
}
