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
 * EventRepository is the main data access class for the entrant side event functionality
 */
public class EventRepository {

    public interface EventsCallback {
        void onUpdate(List<Event> events);
    }

    public interface EventCallback {
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
        
        // Handle both "location" and "locationName" for compatibility
        String location = data.get("location") instanceof String ? (String) data.get("location") : "";
        if (location.isEmpty()) {
            location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        }

        String timeText = formatTimeText(data);
        // Fallback for older data format
        if (timeText.equals("No Date")) {
            Timestamp start = data.get("event_start_time") instanceof Timestamp ? (Timestamp) data.get("event_start_time") : null;
            Timestamp end = data.get("event_end_time") instanceof Timestamp ? (Timestamp) data.get("event_end_time") : null;
            if (start != null && end != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.CANADA);
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.CANADA);
                timeText = dateFormat.format(start.toDate()) + " " + timeFormat.format(start.toDate()) + "-" + timeFormat.format(end.toDate());
            }
        }

        long waitingCount = 0L;
        Object waitingObj = data.get("waiting_count");
        if (waitingObj instanceof Number) {
            waitingCount = ((Number) waitingObj).longValue();
        } else if (data.get("waitingCount") instanceof Number) {
            waitingCount = ((Number) data.get("waitingCount")).longValue();
        }

        String posterUrl = data.get("posterUriString") instanceof String ? (String) data.get("posterUriString") : null;
        if (posterUrl == null) {
            posterUrl = data.get("posterUrl") instanceof String ? (String) data.get("posterUrl") : null;
        }

        return new Event(documentId, name, location, timeText, waitingCount, posterUrl);
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

    public ListenerRegistration listenToOpenEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
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

    public void joinWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onAlreadyJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
                    if (waitlistDoc.exists()) {
                        throw new IllegalStateException("ALREADY_JOINED");
                    }
                    DocumentSnapshot eventDoc = transaction.get(eventRef);
                    Long currentCount = eventDoc.getLong("waiting_count");
                    if (currentCount == null) currentCount = eventDoc.getLong("waitingCount");
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
                    if (currentCount == null) currentCount = eventDoc.getLong("waitingCount");
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

    public ListenerRegistration listenToMyEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
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
                });
    }
}
