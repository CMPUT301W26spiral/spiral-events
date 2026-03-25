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

        return new Event(
            documentId, name, location, isPublic, interests, description, geolocation, maxEntrants,
            eventDate, eventStartTime, eventEndTime, drawDate, drawStartTime, drawEndTime,
            posterUrl, eventCreated, timeText, waitingCount, organizerId
        );
    }

    private Event toEvent(String documentId, Map<String, Object> data) {
        String name = data.get("name") instanceof String ? (String) data.get("name") : "";
        String location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        long waitingCount = data.get("waiting_count") instanceof Number ? ((Number) data.get("waiting_count")).longValue() : 0L;
        String posterUrl = (String) data.get("posterUriString");
        String organizerId = (String) data.get("organizerId");

        String timeText = data.get("timeText") instanceof String ? (String) data.get("timeText") : "";
        
        // Fallback: If timeText is missing, format it from timestamps if available
        if (timeText.isEmpty() && data.get("event_start_time") instanceof Timestamp) {
            Timestamp start = (Timestamp) data.get("event_start_time");
            Timestamp end = (Timestamp) data.get("event_end_time");
            if (start != null && end != null) {
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.CANADA);
                java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.CANADA);
                java.util.Date startDate = start.toDate();
                timeText = df.format(startDate) + " " + tf.format(startDate) + "-" + tf.format(end.toDate());
            }
        }

        String description = data.get("description") instanceof String ? (String) data.get("description") : "";
        boolean isPublic = data.get("isPublic") instanceof Boolean ? (Boolean) data.get("isPublic") : true;

        Integer maxEntrants = null;
        if (data.get("maxEntrants") instanceof Number) {
            maxEntrants = ((Number) data.get("maxEntrants")).intValue();
        }

        // Fetching more fields to ensure rules (draw info) and other details are displayed
        String interests = data.get("interests") instanceof String ? (String) data.get("interests") : "";
        String geolocation = data.get("geolocation") instanceof String ? (String) data.get("geolocation") : "";
        String eventDate = data.get("eventDate") instanceof String ? (String) data.get("eventDate") : "";
        String eventStartTime = data.get("eventStartTime") instanceof String ? (String) data.get("eventStartTime") : "";
        String eventEndTime = data.get("eventEndTime") instanceof String ? (String) data.get("eventEndTime") : "";
        String drawDate = data.get("drawDate") instanceof String ? (String) data.get("drawDate") : "";
        String drawStartTime = data.get("drawStartTime") instanceof String ? (String) data.get("drawStartTime") : "";
        String drawEndTime = data.get("drawEndTime") instanceof String ? (String) data.get("drawEndTime") : "";
        String eventCreated = data.get("eventCreated") instanceof String ? (String) data.get("eventCreated") : "";

        return new Event(documentId, name, location, isPublic, interests, description, geolocation, maxEntrants, 
                         eventDate, eventStartTime, eventEndTime, drawDate, drawStartTime, drawEndTime, 
                         posterUrl, eventCreated, timeText, waitingCount, organizerId);
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

//trying something wow i hope this fixes it for temi poor guy
