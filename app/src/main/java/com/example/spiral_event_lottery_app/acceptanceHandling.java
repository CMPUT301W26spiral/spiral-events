package com.example.spiral_event_lottery_app;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This is handling the backend Firebase Firestore operations for accepting event invitations.
 * @author Abdul Haq Bin Abdul Rehman
 */
public class acceptanceHandling {
    private FirebaseFirestore db_identify;
    public acceptanceHandling(){
        db_identify = FirebaseFirestore.getInstance();
    }
    /**
     * Updates an entrant's status to "Accepted" in Firebase.
     * @param context   This is the activity context for Toasts.
     * @param event_id  This is the firebase doc ID for the event.
     * @param device_id It is the unique ID of the entrant/user device.
     */
    public void invitation_accepted(Context context, String event_id, String device_id) {
            if (event_id == null || event_id.isEmpty() || device_id == null || device_id.isEmpty()) {
                return; // so don't hit firebase with garbage data
            }
        DocumentReference doc_path = db_identify.collection("Events").document(event_id)
                .collection("Entrants").document(device_id);
        doc_path.update("Status", "Accepted").addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Successfully joined!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to join event", Toast.LENGTH_SHORT).show();
                });
    }
}
