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
 * EventRepository handles real-time data for entrants.
 * Fixes stale data issues by avoiding collectionGroups for primary joined event logic.
 */
public class EventRepository {

    /** Callback for multiple events */
    public interface EventsCallback { void onUpdate(List<Event> events); }
    /** Callback for a single event */
    public interface EventCallback { void onUpdate(Event event); }
    /** Callback for boolean results */
    public interface BooleanCallback { void onResult(boolean value); }
    /** Callback for string status results */
    public interface StatusCallback { void onStatus(String status); }
    /** Callback for error handling */
    public interface ErrorCallback { void onError(Exception e); }
    /** Callback for generic success */
    public interface SuccessCallback { void onSuccess(); }
    /** Callback for list of user IDs */
    public interface UserIdsCallback { void onUpdate(List<String> userIds); }

    private final FirebaseFirestore db;
    private final String deviceId;

    /**
     * Constructs a new EventRepository.
     * @param context Application context used to retrieve device ID.
     */
    public EventRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        deviceId = DeviceIdProvider.getDeviceId(context);
    }

    /**
     * Listens for all events where the current user is registered.
     * Includes waitlist, selected_list, and canceled_list.
     *
     * @param onUpdate Callback invoked when the joined events list is updated.
     * @param onError Callback invoked if an error occurs.
     * @return A ListenerRegistration to manage the lifecycle of the listener.
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
                                        // NEW: Check canceled_list
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

    /**
     * Internal helper to synchronize the completion of multiple asynchronous Firestore lookups.
     * Sorts results alphabetically before returning.
     */
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
     *
     * @param eventId The ID of the event for redraw.
     * @param eventName The name of the event for notification content.
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

    /** Helper to create a two-key map. */
    private Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Converts a raw Firestore data map into a structured Event model.
     * Handles type safety and default values.
     */
    private Event toEvent(String documentId, Map<String, Object> data) {
        String name = data.get("name") instanceof String ? (String) data.get("name") : "";
        String location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        long waitingCount = data.get("waiting_count") instanceof Number ? ((Number) data.get("waiting_count")).longValue() : 0L;
        String posterUrl = (String) data.get("posterUriString");
        String organizerId = (String) data.get("organizerId");
        String timeText = data.get("timeText") instanceof String ? (String) data.get("timeText") : "";

        Boolean isPublic = data.get("isPublic") instanceof Boolean ? (Boolean) data.get("isPublic") : true;
        String description = (String) data.get("description");
        Integer maxEntrants = data.get("maxEntrants") instanceof Number ? ((Number) data.get("maxEntrants")).intValue() : null;
        Boolean lotteryDone = data.get("lottery_done") instanceof Boolean ? (Boolean) data.get("lottery_done") : false;

        return new Event(documentId, name, location, isPublic, "", description != null ? description : "", "", maxEntrants, "", "", "", "", "", "", posterUrl, "", timeText, waitingCount, organizerId, lotteryDone);
    }

    /**
     * Listens for all public events available for the Home screen.
     * @param onUpdate Callback for updated event list.
     * @param onError Callback for errors.
     * @return ListenerRegistration object.
     */
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

    /**
     * Listens for real-time updates to a specific event.
     * @param eventId The event ID.
     * @param onUpdate Callback for the updated Event.
     * @param onError Callback for errors.
     * @return ListenerRegistration object.
     */
    public ListenerRegistration listenToEvent(String eventId, final EventCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events").document(eventId).addSnapshotListener((snapshot, error) -> {
            if (error != null) { onError.onError(error); return; }
            if (snapshot != null && snapshot.exists()) onUpdate.onUpdate(toEvent(snapshot.getId(), snapshot.getData()));
            else onUpdate.onUpdate(null);
        });
    }

    /**
     * Checks if the current user is in the waitlist for a specific event.
     */
    public void isJoined(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection("waitlist").document(deviceId).get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    /**
     * Retrieves the selection status (e.g., "invited", "accepted") for the current user.
     */
    public void getWinnerStatus(String eventId, final StatusCallback callback) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> callback.onStatus(doc.exists() ? doc.getString("status") : null));
    }

    /**
     * Checks if the current user is in the selected_list for an event.
     */
    public void isSelected(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    /**
     * Joins the waitlist for an event.
     * Prevents joining if already in the waitlist or already selected.
     * Standardizes count updates via Firestore transactions.
     *
     * @param eventId The event ID.
     * @param onSuccess Callback for success.
     * @param onAlreadyJoined Callback if user is already joined.
     * @param onError Callback for errors.
     */
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
                    } else {
                        onError.onError(e);
                    }
                });
    }

    /**
     * Leaves the waitlist for an event.
     */
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
