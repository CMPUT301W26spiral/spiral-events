package com.example.event_creation;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.spiral_event_lottery_app.model.Event;

import java.io.InputStream;
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
     * Adds an event and uploads its poster if it has a local URI.
     * @param context Context required to open the URI stream.
     * @param event The event to add.
     */
    public void addEvent(Context context, Event event) {
        eventList.add(event);
        
        String uriString = event.getPosterUriString();
        if (uriString != null && (uriString.startsWith("content://") || uriString.startsWith("file://"))) {
            uploadPosterAndSave(context, event);
        } else {
            saveToFirestore(event);
        }
    }

    private void uploadPosterAndSave(Context context, Event event) {
        try {
            Uri fileUri = Uri.parse(event.getPosterUriString());
            InputStream stream = context.getContentResolver().openInputStream(fileUri);
            
            if (stream == null) {
                Log.e(TAG, "Could not open input stream for URI: " + fileUri);
                event.setPosterUriString(null); // Clear broken local URI
                saveToFirestore(event);
                return;
            }

            String fileName = "event_posters/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference().child(fileName);

            Log.d(TAG, "Uploading image via stream: " + fileName);

            storageRef.putStream(stream)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.d(TAG, "Upload successful! URL: " + uri.toString());
                            event.setPosterUriString(uri.toString());
                            saveToFirestore(event);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Upload failed", e);
                        event.setPosterUriString(null); // Don't save local URI to DB
                        saveToFirestore(event);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            event.setPosterUriString(null);
            saveToFirestore(event);
        }
    }

    private void saveToFirestore(Event event) {
        if (event.getId() != null && !event.getId().isEmpty()) {
            db.collection("events").document(event.getId()).set(event);
        } else {
            db.collection("events").add(event)
                .addOnSuccessListener(doc -> {
                    event.setId(doc.getId());
                    doc.update("id", doc.getId());
                });
        }
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventList);
    }
}
