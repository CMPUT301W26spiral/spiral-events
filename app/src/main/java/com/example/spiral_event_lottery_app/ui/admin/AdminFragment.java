package com.example.spiral_event_lottery_app.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
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
 * @author Abdul Haq Bin Abdul Rehman
 */
public class AdminFragment extends Fragment {

    private static final String TAG = "AdminFragment";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private AdminAdapter adapter;
    private List<DocumentSnapshot> currentList;
    private String currentMode = "EVENTS";

    private Button btnEvents, btnProfiles, btnImages, btnNotifLogs, btnComments;

    public AdminFragment() {}

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

        // Find buttons
        btnEvents = view.findViewById(R.id.btn_admin_events);
        btnProfiles = view.findViewById(R.id.btn_admin_profiles);
        btnImages = view.findViewById(R.id.btn_admin_images);
        btnNotifLogs = view.findViewById(R.id.btn_admin_notif_logs);
        btnComments = view.findViewById(R.id.btn_admin_comments);

        // Wire up browse buttons
        btnEvents.setOnClickListener(v -> loadCollection("events", "EVENTS"));
        btnProfiles.setOnClickListener(v -> loadCollection("users", "PROFILES"));
        btnImages.setOnClickListener(v -> loadImages());
        btnNotifLogs.setOnClickListener(v -> loadCollection("notifications", "NOTIFICATIONS"));
        btnComments.setOnClickListener(v -> loadAllComments());

        // Default view
        loadCollection("events", "EVENTS");
    }

    /**
     * Highlights the active navigation button to indicate the current view mode.
     * @param mode The current display mode.
     */
    private void highlightButton(String mode) {
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.primary_green);
        int defaultColor = Color.parseColor("#E0E0E0"); // Neutral light gray

        btnEvents.setBackgroundTintList(ColorStateList.valueOf(mode.equals("EVENTS") ? selectedColor : defaultColor));
        btnProfiles.setBackgroundTintList(ColorStateList.valueOf(mode.equals("PROFILES") ? selectedColor : defaultColor));
        btnImages.setBackgroundTintList(ColorStateList.valueOf(mode.equals("IMAGES") ? selectedColor : defaultColor));
        btnNotifLogs.setBackgroundTintList(ColorStateList.valueOf(mode.equals("NOTIFICATIONS") ? selectedColor : defaultColor));
        btnComments.setBackgroundTintList(ColorStateList.valueOf(mode.equals("COMMENTS") ? selectedColor : defaultColor));

        // Adjust text color for contrast
        int white = Color.WHITE;
        int black = Color.BLACK;
        btnEvents.setTextColor(mode.equals("EVENTS") ? white : black);
        btnProfiles.setTextColor(mode.equals("PROFILES") ? white : black);
        btnImages.setTextColor(mode.equals("IMAGES") ? white : black);
        btnNotifLogs.setTextColor(mode.equals("NOTIFICATIONS") ? white : black);
        btnComments.setTextColor(mode.equals("COMMENTS") ? white : black);
    }

    private void loadAllComments() {
        currentMode = "COMMENTS";
        highlightButton(currentMode);
        db.collectionGroup("comments")
                .limit(100)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful() && task.getResult() != null) {
                        currentList = task.getResult().getDocuments();
                        adapter.updateData(currentList, "COMMENTS");
                        if (currentList.isEmpty()) {
                            Toast.makeText(requireContext(), "No comments found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Failed to load comments", task.getException());
                        Toast.makeText(requireContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCollection(String collectionName, String mode) {
        currentMode = mode;
        highlightButton(currentMode);
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

    private void loadImages() {
        currentMode = "IMAGES";
        highlightButton(currentMode);
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

    private void confirmAndDelete(DocumentSnapshot document, String mode, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to permanently remove this item?")
                .setPositiveButton("Remove", (dialog, which) -> deleteItem(document, mode, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem(DocumentSnapshot document, String mode, int position) {
        if (mode.equals("IMAGES")) {
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

    private void onDeleteSuccess(int position) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Removed successfully", Toast.LENGTH_SHORT).show();
        if (position < currentList.size()) {
            currentList.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }
}