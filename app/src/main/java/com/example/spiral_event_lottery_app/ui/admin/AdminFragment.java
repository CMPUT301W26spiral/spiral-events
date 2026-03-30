package com.example.spiral_event_lottery_app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spiral_event_lottery_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminFragment is the central control panel for admin operations.
 * It is only reachable when the signed-in user has isAdmin=true in Firestore.
 *
 * Fulfils:
 *   US 03.01.01 – Remove events
 *   US 03.02.01 – Remove profiles
 *   US 03.03.01 – Remove images
 *   US 03.04.01 – Browse events
 *   US 03.05.01 – Browse profiles
 *   US 03.06.01 – Browse images
 *   US 03.07.01 – Remove organizers (profiles)
 *   US 03.08.01 – Review notification logs
 *   US 03.09.01 – Admin retains normal bottom nav (organizer/entrant access)
 *   US 03.10.01 – Remove event comments
 *
 * @author Abdul Haq Bin Abdul Rehman
 */
public class AdminFragment extends Fragment {

    private static final String TAG = "AdminFragment";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private AdminAdapter adapter;
    private List<DocumentSnapshot> currentList;
    private String currentMode = "EVENTS";

    /**
     * Required empty constructor for fragment instantiation.
     */
    public AdminFragment() {}

    /**
     * Factory method — use this instead of the constructor.
     * @return A new AdminFragment instance.
     */
    public static AdminFragment newInstance() {
        return new AdminFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    /**
     * Sets up the RecyclerView, adapter, and button click listeners.
     * Loads the events list by default when the fragment opens.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentList = new ArrayList<>();

        RecyclerView recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AdminAdapter(currentList, currentMode, this::confirmAndDelete);
        recyclerView.setAdapter(adapter);

        // Wire up browse buttons
        view.findViewById(R.id.btn_admin_events).setOnClickListener(v -> loadCollection("events", "EVENTS"));
        view.findViewById(R.id.btn_admin_profiles).setOnClickListener(v -> loadCollection("users", "PROFILES"));
        view.findViewById(R.id.btn_admin_images).setOnClickListener(v -> loadImages());
        view.findViewById(R.id.btn_admin_notif_logs).setOnClickListener(v -> loadCollection("notifications", "NOTIFICATIONS"));
        view.findViewById(R.id.btn_admin_comments).setOnClickListener(v ->
                db.collectionGroup("comments").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        currentList = task.getResult().getDocuments();
                        currentMode = "COMMENTS";
                        adapter.updateData(currentList, currentMode);
                    }
                }));

        // Default view
        loadCollection("events", "EVENTS");
    }

    /**
     * Fetches all documents from a root-level Firestore collection and updates the list.
     *
     * @param collectionName Firestore collection name (e.g. "events", "users").
     * @param mode           Display mode string passed to the adapter.
     */
    private void loadCollection(String collectionName, String mode) {
        currentMode = mode;
        db.collection(collectionName).get().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful() && task.getResult() != null) {
                currentList = task.getResult().getDocuments();
                adapter.updateData(currentList, mode);
            } else {
                Log.e(TAG, "Failed to load " + collectionName, task.getException());
                Toast.makeText(requireContext(), "Failed to load " + mode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads events that have a posterUriString field for the image browsing view.
     * Fulfils US 03.06.01.
     */
    private void loadImages() {
        currentMode = "IMAGES";
        db.collection("events")
                .whereNotEqualTo("posterUriString", null)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful() && task.getResult() != null) {
                        currentList = task.getResult().getDocuments();
                        adapter.updateData(currentList, "IMAGES");
                    }
                });
    }

    /**
     * Shows a confirmation dialog before permanently deleting an item.
     * Prevents accidental removals.
     *
     * @param document The Firestore document to remove.
     * @param mode     Current display mode.
     * @param position Adapter position of the item.
     */
    private void confirmAndDelete(DocumentSnapshot document, String mode, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to permanently remove this item?")
                .setPositiveButton("Remove", (dialog, which) -> deleteItem(document, mode, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes a Firestore document based on the current display mode.
     *
     * For IMAGES: removes the posterUriString field from the event document
     *             and deletes the file from Firebase Storage.
     * For PROFILES/ORGANIZERS: deletes the user document (US 03.02.01, US 03.07.01).
     * For EVENTS: deletes the event document (US 03.01.01).
     * For COMMENTS: deletes the comment document (US 03.10.01).
     *
     * @param document The Firestore document to remove.
     * @param mode     Current display mode.
     * @param position Adapter position of the item.
     */
    private void deleteItem(DocumentSnapshot document, String mode, int position) {
        if (mode.equals("IMAGES")) {
            // Remove poster from Storage then clear the field in Firestore
            String imageUrl = document.getString("posterUriString");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    storage.getReferenceFromUrl(imageUrl).delete();
                } catch (Exception e) {
                    Log.e(TAG, "Could not delete from storage", e);
                }
            }
            db.collection("events").document(document.getId())
                    .update("posterUriString", null)
                    .addOnSuccessListener(v -> onDeleteSuccess(position));
            return;
        }

        String collection;
        switch (mode) {
            case "PROFILES": collection = "users"; break;
            case "COMMENTS":
                // Comments are a subcollection — use the document's own reference
                document.getReference().delete()
                        .addOnSuccessListener(v -> onDeleteSuccess(position))
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                return;
            default: collection = "events"; break; // EVENTS
        }

        db.collection(collection).document(document.getId()).delete()
                .addOnSuccessListener(v -> onDeleteSuccess(position))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes the item from the local list and notifies the adapter.
     * @param position Adapter position of the deleted item.
     */
    private void onDeleteSuccess(int position) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Removed successfully", Toast.LENGTH_SHORT).show();
        if (position < currentList.size()) {
            currentList.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }
}