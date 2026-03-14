package com.example.event_creation;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.spiral_event_lottery_app.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Singleton class to manage a list of events.
 * Now integrated with Firebase Firestore and Storage.
 */
public class EventManager {
    private static final String TAG = "EventManager";
    private static EventManager instance;
    private final List<Event> eventList;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private EventManager() {
        eventList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    /**
     * Adds an event to local list and syncs it to Firebase.
     * Sets the event ID before saving.
     * @param event The event to add.
     */
    public void addEvent(Event event) {
        // Pre-generate the ID so we can use it immediately for QR codes
        DocumentReference docRef = db.collection("events").document();
        event.setId(docRef.getId());
        
        eventList.add(event);
        
        // If there's a local poster URI, upload it first, then save to Firestore
        if (event.getPosterUriString() != null) {
            uploadPosterAndSave(event, docRef);
        } else {
            saveToFirestore(event, docRef);
        }
    }

    private void uploadPosterAndSave(Event event, DocumentReference docRef) {
        Uri file = Uri.parse(event.getPosterUriString());
        StorageReference storageRef = storage.getReference().child("event_posters/" + UUID.randomUUID().toString());

        storageRef.putFile(file)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Replace local URI with permanent download URL
                    event.setPosterUriString(uri.toString());
                    saveToFirestore(event, docRef);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    // Save anyway without the image if upload fails
                    saveToFirestore(event, docRef);
                });
    }

    private void saveToFirestore(Event event, DocumentReference docRef) {
        docRef.set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event added with ID: " + event.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding event", e));
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventList);
    }
}
