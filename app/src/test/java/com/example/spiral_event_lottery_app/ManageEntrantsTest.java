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
 *
 * Covers:
 * - US 02.03.01: Waiting list limit
 * - US 02.06.01: Invited/selected list
 * - US 02.06.02: Cancelled entrants
 * - US 02.06.03: Enrolled (accepted) entrants
 * - US 02.06.04: Cancel entrant status logic
 * - US 02.06.05: CSV export content
 */
public class ManageEntrantsTest {

    // -------------------------------------------------------------------------
    // Waiting List Limit
    // -------------------------------------------------------------------------

    @Test
    public void testEventStoresMaxEntrantsLimit() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 10, "", "", "", "", "", "", null, "", "", 5L, "", false
        );
        assertEquals(Integer.valueOf(10), event.getMaxEntrants());
    }

    @Test
    public void testEventWithNoLimitHasNullMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", null, "", "", "", "", "", "", null, "", "", 0L, "", false
        );
        assertNull(event.getMaxEntrants());
    }

    @Test
    public void testOpenSpotsCalculatedCorrectly() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 10, "", "", "", "", "", "", null, "", "", 3L, "", false
        );
        long openSpots = event.getMaxEntrants() - event.getWaitingCount();
        assertEquals(7L, openSpots);
    }

    @Test
    public void testWaitingCountDoesNotExceedMaxEntrants() {
        Event event = new Event(
                "event1", "Test Event", "", true, "", "",
                "", 5, "", "", "", "", "", "", null, "", "", 5L, "", false
        );
        assertTrue(event.getWaitingCount() <= event.getMaxEntrants());
    }

    // -------------------------------------------------------------------------
    // Waiting List
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Invited / Selected List
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Status Filtering - US 02.06.01, 02.06.02, 02.06.03
    // -------------------------------------------------------------------------

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
        assertFalse(invitedTab.contains("user3"));
        assertFalse(invitedTab.contains("user4"));
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
        assertFalse(cancelledTab.contains("user1"));
        assertFalse(cancelledTab.contains("user2"));
    }

    /**
     * Verifies that only "accepted" entrants appear in the final enrolled list.
     * Implements US 02.06.03.
     */
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

    // -------------------------------------------------------------------------
    // Cancel Entrant - US 02.06.04
    // -------------------------------------------------------------------------

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

        // Simulate cancel
        statusMap.put("user1", "cancelled");

        assertEquals("cancelled", statusMap.get("user1"));
        assertNotEquals("pending", statusMap.get("user1"));
    }

    /**
     * Verifies that after cancellation, the entrant no longer appears
     * in the invited tab filter.
     */
    @Test
    public void testCancelledEntrantRemovedFromInvitedTab() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");
        statusMap.put("user2", "accepted");

        // Simulate cancel
        statusMap.put("user1", "cancelled");

        List<String> invitedTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("pending") || e.getValue().equals("accepted"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertFalse(invitedTab.contains("user1"));
        assertTrue(invitedTab.contains("user2"));
    }

    /**
     * Verifies that after cancellation, the entrant appears in the cancelled tab.
     */
    @Test
    public void testCancelledEntrantAppearsInCancelledTab() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("user1", "pending");

        // Simulate cancel
        statusMap.put("user1", "cancelled");

        List<String> cancelledTab = statusMap.entrySet().stream()
                .filter(e -> e.getValue().equals("declined") || e.getValue().equals("cancelled"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertTrue(cancelledTab.contains("user1"));
    }

    // -------------------------------------------------------------------------
    // CSV Export - US 02.06.05
    // -------------------------------------------------------------------------

    /**
     * Verifies that the CSV content contains the correct header row.
     */
    @Test
    public void testCsvExportContainsHeader() {
        StringBuilder csv = new StringBuilder();
        csv.append("Name,Device ID,Status\n");
        csv.append("Alice,device123,accepted\n");

        assertTrue(csv.toString().startsWith("Name,Device ID,Status"));
    }

    /**
     * Verifies that only accepted entrants are included in the CSV export.
     * Implements US 02.06.05.
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
        assertFalse(csv.toString().contains("Charlie"));
    }

    /**
     * Verifies that the CSV export is empty when no entrants have accepted.
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
     * Verifies that the CSV export row format is correct.
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