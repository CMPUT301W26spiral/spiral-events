package com.example.spiral_event_lottery_app;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Get the unique Android Device ID to identify this Entrant
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        recyclerView = findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        SwitchMaterial optOutSwitch = findViewById(R.id.notification_opt_out_switch);
        optOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                listenForNotifications(currentUserId);
            } else {
                stopListening();
                notificationList.clear();
                adapter.notifyDataSetChanged();
            }
        });

        listenForNotifications(currentUserId);
    }

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
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void stopListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListening();
    }
}
