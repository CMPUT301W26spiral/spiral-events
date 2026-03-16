package com.example.event_creation;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.spiral_event_lottery_app.model.Event;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    public interface OnEventCreatedListener {
        void onSuccess(String eventId);
        void onFailure(Exception e);
    }

    /**
     * Adds an event to local list and syncs it to Firebase.
     * @param event The event to add.
     */
    public void addEvent(Event event, OnEventCreatedListener listener) {
        eventList.add(event);
        
        // Create a new document to get an ID first
        DocumentReference newDoc = db.collection("events").document();
        event.setId(newDoc.getId());
        
        // Generate a random binary string hash (16 bits)
        String qrHash = generateRandomBinaryString(16);
        event.setQrHash(qrHash);
        
        Log.d(TAG, "Creating event with ID: " + event.getId() + " and hash: " + qrHash);

        // 1. Generate and upload QR Code
        generateAndUploadQRCode(event, new OnEventCreatedListener() {
            @Override
            public void onSuccess(String qrUrl) {
                Log.d(TAG, "QR Code uploaded successfully: " + qrUrl);
                event.setQrCodeUrl(qrUrl);
                
                // 2. Upload poster if exists, then save
                if (event.getPosterUriString() != null && 
                    !event.getPosterUriString().isEmpty() && 
                    !event.getPosterUriString().startsWith("http")) {
                    uploadPosterAndSave(event, listener);
                } else {
                    saveToFirestore(event, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "QR Code upload failed", e);
                // Even if QR fails, try to save the event
                saveToFirestore(event, listener);
            }
        });
    }

    private String generateRandomBinaryString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(2));
        }
        return sb.toString();
    }

    private void generateAndUploadQRCode(Event event, OnEventCreatedListener internalListener) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // QR content now includes the hash: spiral-events://event/{eventId}/{qrHash}
            String qrContent = "spiral-events://event/" + event.getId() + "/" + event.getQrHash();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 512, 512);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference qrRef = storage.getReference().child("event_qrcodes/" + event.getId() + ".png");
            qrRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> qrRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        internalListener.onSuccess(uri.toString());
                    }))
                    .addOnFailureListener(internalListener::onFailure);

        } catch (WriterException e) {
            internalListener.onFailure(e);
        }
    }

    private void uploadPosterAndSave(Event event, OnEventCreatedListener listener) {
        Uri file = Uri.parse(event.getPosterUriString());
        StorageReference storageRef = storage.getReference().child("event_posters/" + UUID.randomUUID().toString());

        Log.d(TAG, "Uploading poster: " + event.getPosterUriString());
        storageRef.putFile(file)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Poster uploaded successfully: " + uri.toString());
                    event.setPosterUriString(uri.toString());
                    saveToFirestore(event, listener);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    saveToFirestore(event, listener);
                });
    }

    private void saveToFirestore(Event event, OnEventCreatedListener listener) {
        Log.d(TAG, "Saving event to Firestore. qrCodeUrl: " + event.getQrCodeUrl());
        db.collection("events")
                .document(event.getId())
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event document successfully written!");
                    if (listener != null) listener.onSuccess(event.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing event document", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    public List<Event> getEvents() {
        return new ArrayList<>(eventList);
    }
}
