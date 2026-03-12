package com.example.spiral_event_lottery_app;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
/**
 * Handles backend Firebase Firestore operations for accepting/declining event invitations.
 * Works with both entrants and waitlist collections depending on lottery implementation.
 * @author Abdul Haq Bin Abdul Rehman
 */
public class acceptanceHandling {
    private FirebaseFirestore db_identify;
    
    public acceptanceHandling(){
        db_identify = FirebaseFirestore.getInstance();
    }
    
    /**
     * Updates an entrant's status to Accepted in Firebase.
     * Uses SetOptions.merge() to create document if it doesn't exist yet.
     * 
     * @param context   Activity context for Toasts
     * @param event_id  Firebase document ID for the event
     * @param device_id Unique ID of the entrant/user device
     */
    public void invitation_accepted(Context context, String event_id, String device_id) {
        if (event_id == null || event_id.isEmpty() || device_id == null || device_id.isEmpty()) {
            return; // Don't hit firebase with garbage data
        }
        
        // Try entrants collection first (lottery result location)
        DocumentReference doc_path = db_identify.collection("events").document(event_id)
                .collection("entrants").document(device_id);
        
        Map<String, Object> data = new HashMap<>();
        data.put("Status", "Accepted");
        data.put("device_id", device_id);
        data.put("accepted_at", com.google.firebase.Timestamp.now());
        
        // Use set with merge to create document if needed
        doc_path.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Successfully joined!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to join event", Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Updates an entrant's status to "Declined" in Firebase.
     * Implements US 01.05.03 - declining an invitation.
     * 
     * @param context   Activity context for Toasts
     * @param event_id  Firebase document ID for the event
     * @param device_id Unique ID of the entrant/user device
     */
    public void invitation_declined(Context context, String event_id, String device_id) {
        if (event_id == null || event_id.isEmpty() || device_id == null || device_id.isEmpty()) {
            return; // Don't hit firebase with garbage data
        }
        
        DocumentReference doc_path = db_identify.collection("events").document(event_id)
                .collection("entrants").document(device_id);
        
        Map<String, Object> data = new HashMap<>();
        data.put("Status", "Declined");
        data.put("device_id", device_id);
        data.put("declined_at", com.google.firebase.Timestamp.now());
        
        // Use set with merge to create document if needed
        doc_path.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Invitation declined.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                });
    }
}
