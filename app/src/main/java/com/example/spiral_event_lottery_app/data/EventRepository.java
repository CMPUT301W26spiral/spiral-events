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
 * Updated to support automated redrawing when users decline invitations.
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
     * Automatically picks a new winner from the waitlist to replace someone who declined.
     * Implements automated redraw logic.
     */
    public void triggerAutomaticRedraw(String eventId, String eventName) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        
        eventRef.collection("waitlist").limit(50).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;

            // Pick a random user from the current waitlist
            List<DocumentSnapshot> docs = snapshot.getDocuments();
            DocumentSnapshot winnerDoc = docs.get((int) (Math.random() * docs.size()));
            String newWinnerId = winnerDoc.getId();

            WriteBatch batch = db.batch();
            
            // Move from waitlist to selected_list
            DocumentReference selectedRef = eventRef.collection("selected_list").document(newWinnerId);
            DocumentReference waitlistRef = eventRef.collection("waitlist").document(newWinnerId);
            
            batch.set(selectedRef, mapOf("selectedAt", System.currentTimeMillis(), "status", "invited"));
            batch.delete(waitlistRef);
            
            // Update the main event count
            eventRef.get().addOnSuccessListener(eventDoc -> {
                Long currentCount = eventDoc.getLong("waiting_count");
                if (currentCount != null) {
                    db.collection("events").document(eventId).update("waiting_count", Math.max(0, currentCount - 1));
                }
                
                batch.commit().addOnSuccessListener(aVoid -> {
                    // Notify the new winner
                    NotificationManager.sendNotification(
                        newWinnerId, 
                        "Invitation Accepted", 
                        "A spot opened up! You have been selected for " + eventName + ". Please confirm your attendance.", 
                        "ACCEPTED", 
                        eventName, 
                        eventId
                    );
                });
            });
        });
    }

    private Map<String, Object> mapOf(String key, Object val, String key2, Object val2) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, val);
        map.put(key2, val2);
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
        String location = data.get("location") instanceof String ? (String) data.get("location") : "";
        if (location.isEmpty()) {
            location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        }

        String timeText = formatTimeText(data);
        long waitingCount = 0L;
        Object waitingObj = data.get("waiting_count");
        if (waitingObj instanceof Number) {
            waitingCount = ((Number) waitingObj).longValue();
        }

        String posterUrl = (String) data.get("posterUriString");
        String organizerId = (String) data.get("organizerId");
        
        boolean isPublic = data.get("isPublic") instanceof Boolean ? (Boolean) data.get("isPublic") : true;

        return new Event(
            documentId, name, location, isPublic, "", "", "", null,
            "", "", "", "", "", "",
            posterUrl, "", timeText, waitingCount, organizerId
        );
    }

    public void fetchMyEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        db.collection("events").get().addOnSuccessListener(snapshot -> {
            List<Event> results = new ArrayList<>();
            if (snapshot == null || snapshot.isEmpty()) {
                onUpdate.onUpdate(results);
                return;
            }
            onUpdate.onUpdate(results); 
        });
    }

    public ListenerRegistration listenToOpenEvents(final EventsCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events")
                .whereEqualTo("isPublic", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { onError.onError(error); return; }
                    if (snapshot == null) { onUpdate.onUpdate(new ArrayList<>()); return; }
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        events.add(toEvent(doc.getId(), doc.getData()));
                    }
                    onUpdate.onUpdate(events);
                });
    }

    public ListenerRegistration listenToEvent(String eventId, final EventCallback onUpdate, final ErrorCallback onError) {
        return db.collection("events").document(eventId).addSnapshotListener((snapshot, error) -> {
            if (error != null) { onError.onError(error); return; }
            if (snapshot != null && snapshot.exists()) {
                onUpdate.onUpdate(toEvent(snapshot.getId(), snapshot.getData()));
            } else {
                onUpdate.onUpdate(null);
            }
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
            DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
            if (waitlistDoc.exists()) throw new IllegalStateException("ALREADY_JOINED");
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            Long currentCount = eventDoc.getLong("waiting_count");
            transaction.set(waitlistRef, mapOf("joined_at", Timestamp.now(), "device_id", deviceId));
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
            DocumentSnapshot waitlistDoc = transaction.get(waitlistRef);
            if (!waitlistDoc.exists()) throw new IllegalStateException("NOT_JOINED");
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            Long currentCount = eventDoc.getLong("waiting_count");
            transaction.delete(waitlistRef);
            transaction.update(eventRef, "waiting_count", Math.max(0L, (currentCount != null ? currentCount : 1) - 1));
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }

    public void declineInvitation(String eventId, final SuccessCallback onSuccess, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference selectedRef = eventRef.collection("selected_list").document(deviceId);
        DocumentReference cancelledRef = eventRef.collection("cancelled_list").document(deviceId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.delete(selectedRef);
            transaction.set(cancelledRef, mapOf("cancelledAt", System.currentTimeMillis(), "deviceId", deviceId));
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }
}
