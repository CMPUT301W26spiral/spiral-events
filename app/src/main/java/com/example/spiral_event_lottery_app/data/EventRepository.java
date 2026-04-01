package com.example.spiral_event_lottery_app.data;

import android.content.Context;

import com.example.spiral_event_lottery_app.model.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EventRepository is the main data access class for the entrant side event functionality.
 * Handles real-time data for entrants and fixes stale data issues by avoiding
 * collectionGroups for primary joined event logic.
 */
public class EventRepository {

    public interface EventsCallback { void onUpdate(List<Event> events); }
    public interface EventCallback { void onUpdate(Event event); }
    public interface BooleanCallback { void onResult(boolean value); }
    public interface StatusCallback { void onStatus(String status); }
    public interface ErrorCallback { void onError(Exception e); }
    public interface SuccessCallback { void onSuccess(); }
    public interface UserIdsCallback { void onUpdate(List<String> userIds); }

    private final FirebaseFirestore db;
    private final String deviceId;

    public EventRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        deviceId = DeviceIdProvider.getDeviceId(context);
    }

    private String formatTimeText(Map<String, Object> data) {
        String eventDate = data.get("eventDate") instanceof String ? (String) data.get("eventDate") : "";
        String startTime = data.get("eventStartTime") instanceof String ? (String) data.get("eventStartTime") : "";
        String endTime = data.get("eventEndTime") instanceof String ? (String) data.get("eventEndTime") : "";

        if (eventDate.isEmpty()) return "No Date";
        return eventDate + " " + startTime + " - " + endTime;
    }

    private Event toEvent(String documentId, Map<String, Object> data) {
        String name = data.get("name") instanceof String ? (String) data.get("name") : "";

        String location = data.get("location") instanceof String ? (String) data.get("location") : "";
        if (location.isEmpty()) {
            location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        }

        String timeText = formatTimeText(data);
        if (timeText.equals("No Date")) {
            Timestamp start = data.get("event_start_time") instanceof Timestamp ? (Timestamp) data.get("event_start_time") : null;
            Timestamp end = data.get("event_end_time") instanceof Timestamp ? (Timestamp) data.get("event_end_time") : null;
            if (start != null && end != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.CANADA);
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.CANADA);
                timeText = dateFormat.format(start.toDate()) + " " + timeFormat.format(start.toDate()) + "-" + timeFormat.format(end.toDate());
            } else if (data.containsKey("timeText")) {
                timeText = data.get("timeText") instanceof String ? (String) data.get("timeText") : "";
            }
        }

        long waitingCount = 0L;
        // Standardized to waiting_count in Firestore
        Object waitingObj = data.get("waiting_count");
        if (waitingObj instanceof Number) {
            waitingCount = ((Number) waitingObj).longValue();
        }

        String posterUrl = data.get("posterUriString") instanceof String ? (String) data.get("posterUriString") : null;
        if (posterUrl == null) {
            posterUrl = data.get("posterUrl") instanceof String ? (String) data.get("posterUrl") : null;
        }

        @SuppressWarnings("unchecked")
        List<String> posterUriStrings = (List<String>) data.get("posterUriStrings");
        if (posterUriStrings == null) {
            posterUriStrings = new ArrayList<>();
        }

        String description = data.get("description") instanceof String ? (String) data.get("description") : "";
        String interests = data.get("interests") instanceof String ? (String) data.get("interests") : "";
        String geolocation = data.get("geolocation") instanceof String ? (String) data.get("geolocation") : "";

        Integer maxEntrants = null;
        if (data.get("maxEntrants") instanceof Number) {
            maxEntrants = ((Number) data.get("maxEntrants")).intValue();
        }

        String eventDate = data.get("eventDate") instanceof String ? (String) data.get("eventDate") : "";
        String eventStartTime = data.get("eventStartTime") instanceof String ? (String) data.get("eventStartTime") : "";
        String eventEndTime = data.get("eventEndTime") instanceof String ? (String) data.get("eventEndTime") : "";
        String drawDate = data.get("drawDate") instanceof String ? (String) data.get("drawDate") : "";
        String drawStartTime = data.get("drawStartTime") instanceof String ? (String) data.get("drawStartTime") : "";
        String drawEndTime = data.get("drawEndTime") instanceof String ? (String) data.get("drawEndTime") : "";
        String eventCreated = data.get("eventCreated") instanceof String ? (String) data.get("eventCreated") : "";
        String organizerId = data.get("organizerId") instanceof String ? (String) data.get("organizerId") : "";

        boolean isPublic = true;
        if (data.get("isPublic") instanceof Boolean) {
            isPublic = (Boolean) data.get("isPublic");
        } else if (data.get("public") instanceof Boolean) {
            isPublic = (Boolean) data.get("public");
        }

        boolean lotteryDone = false;
        if (data.get("lottery_done") instanceof Boolean) {
            lotteryDone = (Boolean) data.get("lottery_done");
        }

        Event event = new Event(
                documentId, name, location, isPublic, interests, description, geolocation, maxEntrants,
                eventDate, eventStartTime, eventEndTime, drawDate, drawStartTime, drawEndTime,
                posterUrl, posterUriStrings, eventCreated, timeText, waitingCount, organizerId
        );

        // Apply the lotteryDone property specific to the second file's logic
        event.setLotteryDone(lotteryDone);

        return event;
    }

    public void fetchMyEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        db.collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.getDocuments().isEmpty()) {
                        onUpdate.onUpdate(new ArrayList<>());
                        return;
                    }
                    List<DocumentSnapshot> allEventDocs = snapshot.getDocuments();
                    List<Event> results = new ArrayList<>();
                    final int[] remaining = {allEventDocs.size()};
                    final boolean[] failed = {false};
                    for (DocumentSnapshot doc : allEventDocs) {
                        DocumentReference eventRef = db.collection("events").document(doc.getId());
                        DocumentReference waitlistDocRef = eventRef.collection("waitlist").document(deviceId);
                        waitlistDocRef.get()
                                .addOnSuccessListener(waitlistDoc -> {
                                    if (waitlistDoc.exists()) {
                                        Map<String, Object> map = doc.getData();
                                        if (map == null) map = new HashMap<>();
                                        results.add(toEvent(doc.getId(), map));
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0 && !failed[0]) {
                                        Collections.sort(results, Comparator.comparing(e -> e.getName()));
                                        onUpdate.onUpdate(results);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        onError.onError(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(onError::onError);
    }

    /**
     * Listens for all events where the current user is registered.
     * Includes waitlist, selected_list, and canceled_list.
     */
    public ListenerRegistration listenToMyEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events").addSnapshotListener((snapshot, error) -> {
            if (error != null) { onError.onError(error); return; }
            if (snapshot == null) { onUpdate.onUpdate(new ArrayList<>()); return; }

            List<DocumentSnapshot> allEventDocs = snapshot.getDocuments();
            List<Event> joinedResults = new ArrayList<>();
            final int[] processed = {0};

            if (allEventDocs.isEmpty()) {
                onUpdate.onUpdate(joinedResults);
                return;
            }

            for (DocumentSnapshot eventDoc : allEventDocs) {
                String eventId = eventDoc.getId();

                // Check waitlist
                db.collection("events").document(eventId).collection("waitlist").document(deviceId).get()
                        .addOnSuccessListener(waitlistDoc -> {
                            if (waitlistDoc.exists()) {
                                joinedResults.add(toEvent(eventId, eventDoc.getData()));
                                checkFinished(processed, allEventDocs.size(), joinedResults, onUpdate);
                            } else {
                                // If not in waitlist, check selected_list
                                db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                                        .addOnSuccessListener(selectedDoc -> {
                                            if (selectedDoc.exists()) {
                                                joinedResults.add(toEvent(eventId, eventDoc.getData()));
                                                checkFinished(processed, allEventDocs.size(), joinedResults, onUpdate);
                                            } else {
                                                // Check canceled_list
                                                db.collection("events").document(eventId).collection("canceled_list").document(deviceId).get()
                                                        .addOnSuccessListener(canceledDoc -> {
                                                            if (canceledDoc.exists()) {
                                                                joinedResults.add(toEvent(eventId, eventDoc.getData()));
                                                            }
                                                            checkFinished(processed, allEventDocs.size(), joinedResults, onUpdate);
                                                        });
                                            }
                                        });
                            }
                        });
            }
        });
    }

    private synchronized void checkFinished(int[] counter, int total, List<Event> results, EventsCallback callback) {
        counter[0]++;
        if (counter[0] >= total) {
            Collections.sort(results, Comparator.comparing(Event::getName));
            callback.onUpdate(results);
        }
    }

    /**
     * Automatically picks a RANDOM person from the waitlist and invites them.
     * If the waitlist is empty, no one is drawn.
     */
    public void triggerAutomaticRedraw(String eventId, String eventName) {
        db.collection("events").document(eventId).collection("waitlist").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        // No more people in waitlist, do not redraw.
                        return;
                    }

                    // Pick a random index
                    int randomIndex = new java.util.Random().nextInt(snapshot.size());
                    DocumentSnapshot winnerDoc = snapshot.getDocuments().get(randomIndex);
                    String winnerId = winnerDoc.getId();

                    // Move from waitlist to selected_list
                    Map<String, Object> selectedData = new HashMap<>();
                    selectedData.put("selectedAt", System.currentTimeMillis());
                    selectedData.put("status", "invited");
                    selectedData.put("deviceId", winnerId);

                    db.collection("events").document(eventId).collection("selected_list").document(winnerId)
                            .set(selectedData)
                            .addOnSuccessListener(aVoid -> {
                                // Remove from waitlist
                                db.collection("events").document(eventId).collection("waitlist").document(winnerId).delete();

                                // Update the event's waiting count
                                db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
                                    Long currentCount = eventDoc.getLong("waiting_count");
                                    if (currentCount != null && currentCount > 0) {
                                        db.collection("events").document(eventId).update("waiting_count", currentCount - 1);
                                    }
                                });

                                // Notify the new lucky winner
                                // Ensure NotificationManager is accessible or imported in this package context
                                NotificationManager.sendNotification(
                                        winnerId,
                                        "You're Invited!",
                                        "A spot opened up for " + eventName + "! You have been selected.",
                                        "INVITATION",
                                        eventName,
                                        eventId
                                );
                            });
                });
    }

    public ListenerRegistration listenToOpenEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        // Only fetch events where isPublic is true for the Home screen
        return db.collection("events")
                .whereEqualTo("isPublic", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        onUpdate.onUpdate(new ArrayList<>());
                        return;
                    }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> map = doc.getData();
                        if (map == null) map = new HashMap<>();
                        events.add(toEvent(doc.getId(), map));
                    }
                    onUpdate.onUpdate(events);
                });
    }

    public ListenerRegistration listenToEvent(String eventId, final EventCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events")
                .document(eventId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.onError(error);
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        onUpdate.onUpdate(null);
                        return;
                    }
                    Map<String, Object> map = snapshot.getData();
                    if (map == null) map = new HashMap<>();
                    onUpdate.onUpdate(toEvent(snapshot.getId(), map));
                });
    }

    public void isJoined(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    public void getWinnerStatus(String eventId, final StatusCallback callback) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> callback.onStatus(doc.exists() ? doc.getString("status") : null));
    }

    public void isSelected(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events")
                .document(eventId)
                .collection("selected_list")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    public void joinWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onAlreadyJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);
        DocumentReference selectedRef = eventRef.collection("selected_list").document(deviceId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
                    if (waitlistDoc.exists()) {
                        throw new IllegalStateException("ALREADY_JOINED");
                    }

                    DocumentSnapshot selectedDoc = transaction.get(selectedRef);
                    if (selectedDoc.exists()) {
                        throw new IllegalStateException("ALREADY_SELECTED");
                    }

                    DocumentReference coOrgRef = eventRef
                            .collection("co_organizers")
                            .document(deviceId);

                    DocumentSnapshot coOrgDoc = transaction.get(coOrgRef);
                    if (coOrgDoc.exists()) {
                        throw new IllegalStateException("CO_ORGANIZER");
                    }

                    DocumentSnapshot eventDoc = transaction.get(eventRef);
                    Long currentCount = eventDoc.getLong("waiting_count");
                    if (currentCount == null) currentCount = 0L;
                    Map<String, Object> waitlistData = new HashMap<>();
                    waitlistData.put("joined_at", Timestamp.now());
                    waitlistData.put("device_id", deviceId);
                    transaction.set(waitlistRef, waitlistData);
                    transaction.update(eventRef, "waiting_count", currentCount + 1);
                    return null;
                }).addOnSuccessListener(unused -> onSuccess.onSuccess())
                .addOnFailureListener(e -> {
                    if ("ALREADY_JOINED".equals(e.getMessage())) {
                        onAlreadyJoined.onSuccess();
                    } else if ("ALREADY_SELECTED".equals(e.getMessage())) {
                        onError.onError(new Exception("You have already been selected for this event."));
                    } else if ("CO_ORGANIZER".equals(e.getMessage())) {
                        onError.onError(new Exception("You are a co-organizer and cannot join this event."));
                    } else {
                        onError.onError(e);
                    }
                });
    }

    public void leaveWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onNotJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
                    if (!waitlistDoc.exists()) {
                        throw new IllegalStateException("NOT_JOINED");
                    }
                    DocumentSnapshot eventDoc = transaction.get(eventRef);
                    Long currentCount = eventDoc.getLong("waiting_count");
                    if (currentCount == null) currentCount = 0L;
                    transaction.delete(waitlistRef);
                    transaction.update(eventRef, "waiting_count", Math.max(0L, currentCount - 1));
                    return null;
                }).addOnSuccessListener(unused -> onSuccess.onSuccess())
                .addOnFailureListener(e -> {
                    if ("NOT_JOINED".equals(e.getMessage())) {
                        onNotJoined.onSuccess();
                    } else {
                        onError.onError(e);
                    }
                });
    }

    /**
     * Declines an invitation. Moves the user from selected_list to canceled_list
     * and triggers an automatic redraw to fill the vacant spot.
     */
    public void declineInvitation(String eventId, final SuccessCallback onSuccess, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            // Remove from selected_list
            transaction.delete(eventRef.collection("selected_list").document(deviceId));

            // Also ensure they are removed from waitlist
            transaction.delete(eventRef.collection("waitlist").document(deviceId));

            // Add to canceled_list
            Map<String, Object> data = new HashMap<>();
            data.put("status", "declined");
            data.put("deviceId", deviceId);
            data.put("declinedAt", Timestamp.now());
            transaction.set(eventRef.collection("canceled_list").document(deviceId), data);

            return null;
        }).addOnSuccessListener(unused -> {
            // Fetch event name first to use in notification, then redraw
            db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
                String eventName = doc.getString("name");
                triggerAutomaticRedraw(eventId, eventName != null ? eventName : "the event");
                onSuccess.onSuccess();
            });
        }).addOnFailureListener(onError::onError);
    }

    /**
     * Fetches the list of device IDs from a specific sub-collection of an event.
     * @param eventId The event ID.
     * @param collectionName The sub-collection name (e.g., "waitlist", "selected_list", "canceled_list").
     */
    public void getEntrantIds(String eventId, String collectionName, final UserIdsCallback onUpdate, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection(collectionName).get()
                .addOnSuccessListener(snapshot -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ids.add(doc.getId());
                    }
                    onUpdate.onUpdate(ids);
                })
                .addOnFailureListener(onError::onError);
    }
}