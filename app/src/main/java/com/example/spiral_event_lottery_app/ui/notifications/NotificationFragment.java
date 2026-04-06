package com.example.spiral_event_lottery_app.ui.notifications;

import android.os.Bundle;
import android.provider.Settings;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment that displays a list of notifications for the current user.
 * Users can view updates about lottery results and event entry confirmations.
 * Supports real-time updates from Firestore, swipe-to-delete, and clear all functionality.
 */
public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_notifications, container, false);
    }

    /**
     * Initializes the UI, connects to Firestore, and sets up gesture listeners.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        setupSwipeToDelete();

        Button clearAllButton = view.findViewById(R.id.notification_clear_all_button);
        clearAllButton.setOnClickListener(v -> showClearAllConfirmation());

        listenForNotifications(currentUserId);
    }

    /**
     * Displays a confirmation dialog before deleting all notifications for the user.
     */
    private void showClearAllConfirmation() {
        if (notificationList.isEmpty()) {
            Toast.makeText(requireContext(), "No notifications to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes all notifications associated with the current user's device ID using a Firestore WriteBatch.
     */
    private void clearAllNotifications() {
        WriteBatch batch = db.batch();
        for (Notification notification : notificationList) {
            if (notification.getId() != null) {
                batch.delete(db.collection("notifications").document(notification.getId()));
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "All notifications cleared from Firestore");
            Toast.makeText(requireContext(), "Notifications cleared", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error clearing notifications", e);
            Toast.makeText(requireContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Configures the swipe gesture listener to allow users to delete individual notifications.
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                Notification notification = notificationList.get(position);
                
                if (notification.getId() != null) {
                    db.collection("notifications").document(notification.getId()).delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification deleted from Firestore"))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error deleting notification", e);
                            Toast.makeText(requireContext(), "Failed to delete notification", Toast.LENGTH_SHORT).show();
                        });
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Starts a real-time listener for notifications in Firestore filtered by the recipientId.
     * Notifications are ordered by timestamp in descending order (most recent first).
     * @param userId The unique device ID of the user.
     */
    private void listenForNotifications(String userId) {
        stopListening();

        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        return;
                    }

                    notificationList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notificationList.add(notification);
                        }
                        
                        // Sort by timestamp descending manually to ensure correct order
                        Collections.sort(notificationList, (n1, n2) -> {
                            if (n1.getTimestamp() == null || n2.getTimestamp() == null) return 0;
                            return n2.getTimestamp().compareTo(n1.getTimestamp());
                        });
                    }
                    adapter.notifyDataSetChanged();
                });
    }
    /**
     * Stops the active Firestore snapshot listener to prevent memory leaks and unnecessary data usage.
     */
    private void stopListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
    }
}
