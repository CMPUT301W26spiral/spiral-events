package com.example.event_creation;

import android.net.Uri;
import android.util.Log;

import com.example.spiral_event_lottery_app.data.TagRepository;
import com.example.spiral_event_lottery_app.model.Tag;
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
 * Integrated with Firebase Firestore, Storage, and TagRepository for AI categorization.
 */
public class EventManager {
    private static final String TAG = "EventManager";
    private static EventManager instance;
    private final List<Event> eventList;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final TagRepository tagRepository;

    private EventManager() {
        eventList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        tagRepository = new TagRepository();
    }

    public static synchronized EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    /**
     * Adds an event to local list and syncs it to Firebase.
     * Also processes tags to trigger AI categorization.
     * @param event The event to add.
     */
    public void addEvent(Event event) {
        DocumentReference docRef = db.collection("events").document();
        event.setId(docRef.getId());
        
        eventList.add(event);

        // TRIGGER: Process tags so Gemini can categorize them
        processEventTags(event.getInterests());
        
        if (event.getPosterUriString() != null) {
            uploadPosterAndSave(event, docRef);
        } else {
            saveToFirestore(event, docRef);
        }
    }

    /**
     * Splits the interests string into individual tags and saves them to Firestore.
     * This is what triggers the Cloud Function 'categorizeNewTag'.
     */
    private void processEventTags(String interests) {
        Log.d(TAG, "DEBUG: processEventTags called with: " + interests);
        if (interests == null || interests.isEmpty()) {
            Log.w(TAG, "DEBUG: Interests string was empty!");
            return;
        }

        String[] tags = interests.split(",");
        for (String t : tags) {
            String cleanTagName = t.trim();
            if (!cleanTagName.isEmpty()) {
                // We use TagRepository to save the tag. 
                // Using a 'pending' status so the Cloud Function knows it needs work.
                Tag tag = new Tag(cleanTagName, cleanTagName, new ArrayList<>(), new ArrayList<>(), "pending");
                tagRepository.saveTagAsync(tag);
                Log.d(TAG, "Sent tag to repository for processing: " + cleanTagName);
            }
        }
    }

    private void uploadPosterAndSave(Event event, DocumentReference docRef) {
        Uri file = Uri.parse(event.getPosterUriString());
        StorageReference storageRef = storage.getReference().child("event_posters/" + UUID.randomUUID().toString());

        storageRef.putFile(file)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    event.setPosterUriString(uri.toString());
                    saveToFirestore(event, docRef);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
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
