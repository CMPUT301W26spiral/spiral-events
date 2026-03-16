package com.example.spiral_event_lottery_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.spiral_event_lottery_app.ui.profile.ProfileFragment;
import com.example.spiral_event_lottery_app.ui.events.MyEventsFragment;
import com.example.spiral_event_lottery_app.ui.home.HomeFragment;
import com.example.spiral_event_lottery_app.ui.notifications.NotificationFragment;
import com.example.event_creation.CreateEventActivity;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment;
import com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private final HomeFragment homeFragment = new HomeFragment();
    private final MyEventsFragment eventsFragment = new MyEventsFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeTab = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        fm.beginTransaction().add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, notificationFragment, "notifications").hide(notificationFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, eventsFragment, "events").hide(eventsFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, homeFragment, "home").commit();

        // REFRESH LOGIC: When a 'Details' screen is closed (popped), refresh the visible tab
        fm.addOnBackStackChangedListener(() -> {
            if (activeTab instanceof MyEventsFragment) {
                ((MyEventsFragment) activeTab).refreshData();
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_add) {
                Intent intent = new Intent(this, CreateEventActivity.class);
                startActivity(intent);
                return true;
            }

            Fragment target = null;
            if (itemId == R.id.nav_home) target = homeFragment;
            else if (itemId == R.id.nav_events) target = eventsFragment;
            else if (itemId == R.id.nav_notifications) target = notificationFragment;
            else if (itemId == R.id.nav_account) target = profileFragment;

            if (target != null) {
                if (activeTab == target) {
                    // If clicking the same tab, clear the backstack for that tab
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    switchTab(target);
                }
                return true;
            }
            return false;
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // spiral-events://event/{eventId}/{qrHash}
            String path = data.getPath();
            if (path != null && path.startsWith("/")) {
                String[] segments = path.substring(1).split("/");
                if (segments.length >= 1) {
                    String eventId = segments[0];
                    String scannedHash = segments.length > 1 ? segments[1] : null;
                    validateAndOpenEvent(eventId, scannedHash);
                }
            }
        }
    }

    private void validateAndOpenEvent(String eventId, String scannedHash) {
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedHash = documentSnapshot.getString("qrHash");
                        String organizerId = documentSnapshot.getString("organizerId");
                        String currentUserId = DeviceIdProvider.getDeviceId(this);

                        // If qrHash is present in DB, it must match the scanned one
                        if (storedHash != null && scannedHash != null && !storedHash.equals(scannedHash)) {
                            Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Fragment detailFragment;
                        if (currentUserId.equals(organizerId)) {
                            detailFragment = EventDetailsOFragment.newInstance(eventId);
                        } else {
                            detailFragment = EventDetailsFragment.newInstance(eventId);
                        }

                        fm.beginTransaction()
                                .add(R.id.fragmentContainer, detailFragment, "details_screen")
                                .addToBackStack("details")
                                .commit();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                });
    }

    private void switchTab(Fragment targetTab) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(activeTab);
        
        Fragment details = fm.findFragmentByTag("details_screen");

        if ((targetTab == homeFragment || targetTab == eventsFragment) && 
            (activeTab == homeFragment || activeTab == eventsFragment)) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            details = null;
        } else if (details != null) {
            ft.hide(details);
        }

        ft.show(targetTab);

        if (details != null && details.isAdded() && (targetTab == homeFragment || targetTab == eventsFragment)) {
            if (activeTab == notificationFragment || activeTab == profileFragment) {
                ft.show(details);
            }
        }

        ft.commit();
        activeTab = targetTab;
        
        // Refresh the list whenever we switch to the Events tab
        if (targetTab instanceof MyEventsFragment) {
            ((MyEventsFragment) targetTab).refreshData();
        }
    }
}
