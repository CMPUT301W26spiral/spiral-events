package com.example.spiral_event_lottery_app.data;

import android.content.Context;
import com.example.spiral_event_lottery_app.model.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EventRepository handles real-time data for entrants.
 * Fixes stale data issues by avoiding collectionGroups for primary joined event logic.
 */
public class EventRepository {

    public interface EventsCallback { void onUpdate(List<Event> events); }
    public interface EventCallback { void onUpdate(Event event); }
    public interface BooleanCallback { void onResult(boolean value); }
    public interface StatusCallback { void onStatus(String status); }
    public interface ErrorCallback { void onError(Exception e); }
    public interface SuccessCallback { void onSuccess(); }

    private final FirebaseFirestore db;
    private final String deviceId;

    public EventRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        deviceId = DeviceIdProvider.getDeviceId(context);
    }

    /**
     * Listens for all events where the current user is registered.
     * Uses a direct collection listener for 100% accuracy and zero stale data.
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
                        } else {
                            // If not in waitlist, check selected_list
                            db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                                .addOnSuccessListener(selectedDoc -> {
                                    if (selectedDoc.exists()) {
                                        joinedResults.add(toEvent(eventId, eventDoc.getData()));
                                    }
                                    checkFinished(processed, allEventDocs.size(), joinedResults, onUpdate);
                                });
                            return; // Wait for the second check
                        }
                        checkFinished(processed, allEventDocs.size(), joinedResults, onUpdate);
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

    public void triggerAutomaticRedraw(String eventId, String eventName) {
        db.collection("events").document(eventId).collection("waitlist").limit(1).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) return;
            DocumentSnapshot winnerDoc = snapshot.getDocuments().get(0);
            db.collection("events").document(eventId).collection("selected_list").document(winnerDoc.getId())
                .set(createMap("selectedAt", System.currentTimeMillis(), "status", "invited"))
                .addOnSuccessListener(aVoid -> {
                    db.collection("events").document(eventId).collection("waitlist").document(winnerDoc.getId()).delete();
                    NotificationManager.sendNotification(winnerDoc.getId(), "Invitation Accepted", "A spot opened up for " + eventName + "!", "ACCEPTED", eventName, eventId);
                });
        });
    }

    private Map<String, Object> createMap(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private Event toEvent(String documentId, Map<String, Object> data) {
        String name = data.get("name") instanceof String ? (String) data.get("name") : "";
        String location = data.get("locationName") instanceof String ? (String) data.get("locationName") : "";
        long waitingCount = data.get("waiting_count") instanceof Number ? ((Number) data.get("waiting_count")).longValue() : 0L;
        String posterUrl = (String) data.get("posterUriString");
        String organizerId = (String) data.get("organizerId");
        return new Event(documentId, name, location, true, "", "", "", null, "", "", "", "", "", "", posterUrl, "", "", waitingCount, organizerId);
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

    public void getWinnerStatus(String eventId, final StatusCallback callback) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> callback.onStatus(doc.exists() ? doc.getString("status") : null));
    }

    public void isSelected(String eventId, final BooleanCallback onResult, final ErrorCallback onError) {
        db.collection("events").document(eventId).collection("selected_list").document(deviceId).get()
                .addOnSuccessListener(doc -> onResult.onResult(doc.exists()))
                .addOnFailureListener(onError::onError);
    }

    public void joinWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onAlreadyJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        db.runTransaction(transaction -> {
            if (transaction.get(eventRef.collection("waitlist").document(deviceId)).exists()) throw new IllegalStateException("ALREADY_JOINED");
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            Long currentCount = eventDoc.getLong("waiting_count");
            Map<String, Object> data = new HashMap<>();
            data.put("joined_at", Timestamp.now());
            data.put("device_id", deviceId);
            transaction.set(eventRef.collection("waitlist").document(deviceId), data);
            transaction.update(eventRef, "waiting_count", (currentCount != null ? currentCount : 0) + 1);
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(e -> {
            if ("ALREADY_JOINED".equals(e.getMessage())) onAlreadyJoined.onSuccess();
            else onError.onError(e);
        });
    }

    public void leaveWaitlist(String eventId, final SuccessCallback onSuccess, final SuccessCallback onNotJoined, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        db.runTransaction(transaction -> {
            if (!transaction.get(eventRef.collection("waitlist").document(deviceId)).exists()) throw new IllegalStateException("NOT_JOINED");
            Long currentCount = transaction.get(eventRef).getLong("waiting_count");
            transaction.delete(eventRef.collection("waitlist").document(deviceId));
            transaction.update(eventRef, "waiting_count", Math.max(0L, (currentCount != null ? currentCount : 1) - 1));
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }

    public void declineInvitation(String eventId, final SuccessCallback onSuccess, final ErrorCallback onError) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        db.runTransaction(transaction -> {
            transaction.delete(eventRef.collection("selected_list").document(deviceId));
            Map<String, Object> data = new HashMap<>();
            data.put("status", "declined");
            data.put("deviceId", deviceId);
            transaction.set(eventRef.collection("cancelled_list").document(deviceId), data);
            return null;
        }).addOnSuccessListener(unused -> onSuccess.onSuccess()).addOnFailureListener(onError::onError);
    }
}
