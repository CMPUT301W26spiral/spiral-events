package com.example.spiral_event_lottery_app.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.spiral_event_lottery_app.model.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
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
 * EventRepository is the main data access class for the entrant side event functionality
 * Communicates with Firebase to support the following user stories:
 * 1. Viewing a list of events that can be joined
 * 2. Viewing total entrants on the waiting list
 * 3. Joining the waiting list for an event
 * 4. Leaving the waiting list for an event
 * 5. Viewing the current entrant's joined events in My Events
 *
 * Classes Used:
 * 1. Context - used to access the current device identity
 * 2. DeviceIdProvider - provides a device ID
 * 3. FirebaseFirestore - provides access to Firestore collections
 * 4. DocumentReference - references event and waitlist documents
 * 5. DocumentSnapshot - reads Firestore document data
 * 6. ListenerRegistration - manages live Firestore listeners
 * 7. Transaction - ensures join/leave updates to waiting count
 * 8. Timestamp - handles event time and join timestamps
 * 9. Event - model class representing an event
 *
 * This class is primarily used by:
 * 1. Home Fragment
 * 2. EventDetailsFragment
 * 3. EventDetailsLeaveFragment
 * 4. MyEventsFragment
 *
 */
public class EventRepository {

    public interface EventsCallback {
        /**
         * Called when the list of events is updated

         * @param events the list of events objects
         * Returns a list of events
         */
        void onUpdate(List<Event> events);
    }

    public interface EventCallback {
        /**
         * Returns a single event
         * @param event
         */
        void onUpdate(Event event);
    }

    public interface BooleanCallback {
        void onResult(boolean value);
    }

    public interface ErrorCallback {
        void onError(Exception e);
    }

    public interface SuccessCallback {
        void onSuccess();
    }

    private final FirebaseFirestore db;
    private final String deviceId;

    /**
     * Creates a new EventRepository for the current application context
     *
     * Initializes the Firestore instance and retrieves the current device's entrant identificator
     * @param context
     */
    public EventRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        deviceId = DeviceIdProvider.getDeviceId(context);
    }

    /**
     * Formats the start and end timstamp of an event
     * @param start
     * @param end
     * @return
     */
    private String formatTimeRange(Timestamp start, Timestamp end) {
        if (start == null || end == null) {
            return "No Time";
        }

        java.util.Date startDate = start.toDate();
        java.util.Date endDate = end.toDate();

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.CANADA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.CANADA);

        return dateFormat.format(startDate) + " "
                + timeFormat.format(startDate) + "-"
                + timeFormat.format(endDate);
    }

    /**
     * Converts the Firestore document data to an Event object
     *
     * @param documentId
     * @param data
     * @return
     */
    private Event toEvent(String documentId, Map<String, Object> data) {
        String name = data.get("name") instanceof String ? (String) data.get("name") : "";
        String locationName = data.get("location_name") instanceof String ? (String) data.get("location_name") : "";
        Timestamp startTime = data.get("event_start_time") instanceof Timestamp ? (Timestamp) data.get("event_start_time") : null;
        Timestamp endTime = data.get("event_end_time") instanceof Timestamp ? (Timestamp) data.get("event_end_time") : null;

        long waitingCount = 0L;
        Object waitingObj = data.get("waiting_count");
        if (waitingObj instanceof Number) {
            waitingCount = ((Number) waitingObj).longValue();
        }

        return new Event(
                documentId,
                name,
                locationName,
                formatTimeRange(startTime, endTime),
                waitingCount
        );
    }

    /**
     * Retrieves the list of events that the current entrant has joined
     * Checks every event document and determines whether the current devise has a corresponding waitlist document
     * Used by the My Events page
     * @param onUpdate
     * @param onError
     */
    public void fetchMyEvents(
            final EventsCallback onUpdate,
            final ErrorCallback onError
    ) {
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
                        DocumentReference waitlistDocRef =
                                eventRef.collection("waitlist").document(deviceId);

                        waitlistDocRef.get()
                                .addOnSuccessListener(waitlistDoc -> {
                                    if (waitlistDoc.exists()) {
                                        Map<String, Object> map = doc.getData();
                                        if (map == null) map = new HashMap<>();
                                        results.add(toEvent(doc.getId(), map));
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0 && !failed[0]) {
                                        Collections.sort(results, Comparator.comparing(Event::getName));
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
     * Starts a live Firestore listener for all events in the database
     * Used by the Home page to display the current list of events that entrants can view and join
     * @param onUpdate
     * @param onError
     * @return
     */
    public ListenerRegistration listenToOpenEvents(
            final EventsCallback onUpdate,
            final ErrorCallback onError
    ) {
        return db.collection("events")
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
     * Starts a live Firestore listener for a single event
     * Used by the Event Details page to display the event details

     * @param eventId
     * @param onUpdate
     * @param onError
     * @return
     */
    public ListenerRegistration listenToEvent(
            String eventId,
            final EventCallback onUpdate,
            final ErrorCallback onError
    ) {
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

    /**
     * Checks whether the current entrant is already joined
     * @param eventId
     * @param onResult
     * @param onError
     */
    public void isJoined(
            String eventId,
            final BooleanCallback onResult,
            final ErrorCallback onError
    ) {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(deviceId)
                .get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    /**
     * Adds the current entrant to the waiting list for an event
     * Prevents duplicate join and increments the waiting count
     * @param eventId
     * @param onSuccess
     * @param onAlreadyJoined
     * @param onError
     */
    public void joinWaitlist(
            String eventId,
            final SuccessCallback onSuccess,
            final SuccessCallback onAlreadyJoined,
            final ErrorCallback onError
    ) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
                    if (waitlistDoc.exists()) {
                        throw new IllegalStateException("ALREADY_JOINED");
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
                    } else {
                        onError.onError(e);
                    }
                });
    }

    /**
     * Removes the current entrant from an event's waitlist
     * Prevents duplicate leave and decrements the waiting count
     * @param eventId
     * @param onSuccess
     * @param onNotJoined
     * @param onError
     */
    public void leaveWaitlist(
            String eventId,
            final SuccessCallback onSuccess,
            final SuccessCallback onNotJoined,
            final ErrorCallback onError
    ) {
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
     * Starts a live Firestore listener for all events the current entrant has joined
     * Queries all waitlist documents for the current entrant's device ID
     * Identifies the parent event documents and returns those events to the UI
     * @param onUpdate
     * @param onError
     * @return
     */
    public ListenerRegistration listenToMyEvents(
            final EventsCallback onUpdate,
            final ErrorCallback onError
    ) {
        return db.collectionGroup("waitlist")
                .whereEqualTo("device_id", deviceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.onError(error);
                        return;
                    }

                    if (snapshot == null || snapshot.getDocuments().isEmpty()) {
                        onUpdate.onUpdate(new ArrayList<>());
                        return;
                    }

                    List<DocumentReference> eventRefs = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        DocumentReference parentEvent = doc.getReference().getParent().getParent();
                        if (parentEvent != null) {
                            eventRefs.add(parentEvent);
                        }
                    }

                    if (eventRefs.isEmpty()) {
                        onUpdate.onUpdate(new ArrayList<>());
                        return;
                    }

                    List<Event> results = new ArrayList<>();
                    final int[] remaining = {eventRefs.size()};
                    final boolean[] failed = {false};

                    for (DocumentReference eventRef : eventRefs) {
                        eventRef.get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        Map<String, Object> map = doc.getData();
                                        if (map == null) map = new HashMap<>();
                                        results.add(toEvent(doc.getId(), map));
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0 && !failed[0]) {
                                        Collections.sort(results, Comparator.comparing(Event::getName));
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
                });
    }
}