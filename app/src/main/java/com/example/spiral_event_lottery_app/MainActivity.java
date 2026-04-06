package com.example.spiral_event_lottery_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.spiral_event_lottery_app.ui.profile.ProfileFragment;
import com.example.spiral_event_lottery_app.ui.events.MyEventsFragment;
import com.example.spiral_event_lottery_app.ui.home.HomeFragment;
import com.example.spiral_event_lottery_app.ui.notifications.NotificationFragment;
import com.example.spiral_event_lottery_app.ui.event_creation.CreateEventActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity serves as the primary container for the application's main navigation.
 * It manages the bottom navigation bar and handles fragment transitions between the 
 * Home, My Events, Notifications, and Profile screens. It also handles incoming 
 * intents, such as those from QR code scans.
 */
public class MainActivity extends AppCompatActivity {

    private final HomeFragment homeFragment = new HomeFragment();
    private final MyEventsFragment eventsFragment = new MyEventsFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeTab = homeFragment;

    /**
     * Initializes the activity, sets up the bottom navigation, and handles the initial 
     * fragment setup. It also adds a listener for backstack changes to trigger data 
     * refreshes on visible fragments.
     * 
     * @param savedInstanceState If the activity is being re-initialized after 
     *                           previously being shut down then this Bundle contains 
     *                           the data it most recently supplied in 
     *                           onSaveInstanceState. Otherwise it is null.
     */
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
            } else if (activeTab == homeFragment) {
                refreshHomeFragment();
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

    /**
     * Public method to trigger a refresh of the HomeFragment's data.
     * Used when returning from activities that might change user interests.
     */
    public void refreshHomeFragment() {
        if (homeFragment.isAdded()) {
            homeFragment.loadCurrentUser();
        }
    }

    /**
     * Called when the activity is already running and receives a new intent.
     * 
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Processes incoming intents, specifically handling QR code scan results by 
     * navigating to the corresponding event details screen based on the user's role.
     * 
     * @param intent The intent to process.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String scannedId = intent.getStringExtra("SCAN_RESULT_ID");
        if (scannedId != null) {
            // US 01.06.01 - Open event details when QR is scanned
            // Check if the current user is the organizer of the event
            String currentUserId = com.example.spiral_event_lottery_app.data.DeviceIdProvider.getDeviceId(this);
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(scannedId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String organizerId = documentSnapshot.getString("organizerId");
                            Fragment detailsFragment;
                            
                            if (currentUserId.equals(organizerId)) {
                                // If current user is organizer, go to Organizer Details view
                                detailsFragment = com.example.spiral_event_lottery_app.ui.oevent.EventDetailsOFragment.Companion.newInstance(scannedId);
                            } else {
                                // If standard user, go to Entrant Details view
                                detailsFragment = com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment.Companion.newInstance(scannedId);
                            }
                            
                            if (!isFinishing() && !isDestroyed()) {
                                fm.beginTransaction()
                                        .add(R.id.fragmentContainer, detailsFragment, "details_screen")
                                        .addToBackStack("details")
                                        .commit();
                            }
                        } else {
                            android.widget.Toast.makeText(this, "Event not found", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(this, "Error fetching event: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    });
            
            // Clear the extra so it doesn't trigger again on rotation
            intent.removeExtra("SCAN_RESULT_ID");
        }
    }

    /**
     * Switches the active fragment based on the user's selection in the bottom 
     * navigation bar. Handles backstack management and ensures that the 
     * appropriate data refreshes are triggered.
     * 
     * @param targetTab The fragment to switch to.
     */
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
        } else if (targetTab instanceof HomeFragment) {
            refreshHomeFragment();
        }
    }
}
