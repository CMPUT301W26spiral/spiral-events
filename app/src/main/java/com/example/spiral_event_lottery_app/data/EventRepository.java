package com.example.spiral_event_lottery_app.data;

import android.content.Context;
import android.util.Log;

import com.example.spiral_event_lottery_app.model.Event;
import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

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

    /**
     * Listens for all events where the current user is on the waiting list.
     * This populates the "Current Events" section in My Events.
     */
    public ListenerRegistration listenToMyEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        return db.collectionGroup("waitlist")
                .whereEqualTo("device_id", deviceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        onError.onError(error);
                        return;
                    }
                    if (snapshot == null || snapshot.isEmpty()) {
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
                    for (DocumentReference ref : eventRefs) {
                        ref.get().addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                results.add(toEvent(doc.getId(), doc.getData()));
                            }
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                Collections.sort(results, Comparator.comparing(Event::getName));
                                onUpdate.onUpdate(results);
                            }
                        }).addOnFailureListener(e -> {
                            remaining[0]--;
                            if (remaining[0] == 0) onUpdate.onUpdate(results);
                        });
                    }
                });
    }

    public void triggerAutomaticRedraw(String eventId, String eventName) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.collection("waitlist").limit(1).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;
            DocumentSnapshot winnerDoc = snapshot.getDocuments().get(0);
            String newWinnerId = winnerDoc.getId();
            WriteBatch batch = db.batch();
            DocumentReference selectedRef = eventRef.collection("selected_list").document(newWinnerId);
            DocumentReference waitlistRef = eventRef.collection("waitlist").document(newWinnerId);
            batch.set(selectedRef, createMap("selectedAt", System.currentTimeMillis(), "status", "invited"));
            batch.delete(waitlistRef);
            eventRef.get().addOnSuccessListener(eventDoc -> {
                Long currentCount = eventDoc.getLong("waiting_count");
                if (currentCount != null) {
                    db.collection("events").document(eventId).update("waiting_count", Math.max(0, currentCount - 1));
                }
                batch.commit().addOnSuccessListener(aVoid -> {
                    NotificationManager.sendNotification(newWinnerId, "Invitation Accepted", 
                        "A spot opened up! You have been selected for " + eventName + ".", "ACCEPTED", eventName, eventId);
                });
            });
        });
    }

    private Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
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
        String location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        String timeText = formatTimeText(data);
        long waitingCount = data.get("waiting_count") instanceof Number ? ((Number) data.get("waiting_count")).longValue() : 0L;
        String posterUrl = (String) data.get("posterUriString");
        String organizerId = (String) data.get("organizerId");
        boolean isPublic = data.get("isPublic") instanceof Boolean ? (Boolean) data.get("isPublic") : true;
        return new Event(documentId, name, location, isPublic, "", "", "", null, "", "", "", "", "", "", posterUrl, "", timeText, waitingCount, organizerId);
    }

    public ListenerRegistration listenToOpenEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events").whereEqualTo("isPublic", true).addSnapshotListener((snapshot, error) -> {
            if (error != null) { onError.onError(error); return; }
            List<Event> events = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) events.add(toEvent(doc.getId(), doc.getData()));
            }
            onUpdate.onUpdate(events);
        });
    }

    public ListenerRegistration listenToEvent(String eventId, final EventCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events").document(eventId).addSnapshotListener((snapshot, error) -> {
            if (error != null) { onError.onError(error); return; }
            if (snapshot != null && snapshot.exists()) onUpdate.onUpdate(toEvent(snapshot.getId(), snapshot.getData()));
            else onUpdate.onUpdate(null);
        });
    }

    public void isJoined(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection("waitlist").document(deviceId).get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    public void isSelected(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    public void joinWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onAlreadyJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            if (transaction.get(waitlistRef).exists()) throw new IllegalStateException("ALREADY_JOINED");
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            Long currentCount = eventDoc.getLong("waiting_count");
            Map<String, Object> data = new HashMap<>();
            data.put("joined_at", Timestamp.now());
            data.put("device_id", deviceId);
            transaction.set(waitlistRef, data);
            transaction.update(eventRef, "waiting_count", (currentCount != null ? currentCount : 0) + 1);
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(e -> {
            if ("ALREADY_JOINED".equals(e.getMessage())) onAlreadyJoined.onSuccess();
            else onError.onError(e);
        });
    }

    public void leaveWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onNotJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitlistRef = eventRef.collection("waitlist").document(deviceId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            if (!transaction.get(waitlistRef).exists()) throw new IllegalStateException("NOT_JOINED");
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            Long currentCount = eventDoc.getLong("waiting_count");
            transaction.delete(waitlistRef);
            transaction.update(eventRef, "waiting_count", Math.max(0L, (currentCount != null ? currentCount : 1) - 1));
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }

    public void declineInvitation(String eventId, final SuccessCallback onSuccess, final ErrorCallback onError) {
        DocumentReference selectedRef = db.collection("events").document(eventId).collection("selected_list").document(deviceId);
        DocumentReference cancelledRef = db.collection("events").document(eventId).collection("cancelled_list").document(deviceId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.delete(selectedRef);
            Map<String, Object> data = new HashMap<>();
            data.put("cancelledAt", System.currentTimeMillis());
            data.put("deviceId", deviceId);
            transaction.set(cancelledRef, data);
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }
}
