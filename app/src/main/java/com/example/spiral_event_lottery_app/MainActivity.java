package com.example.spiral_event_lottery_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.spiral_event_lottery_app.ui.ProfileFragment;
import com.example.spiral_event_lottery_app.ui.events.MyEventsFragment;
import com.example.spiral_event_lottery_app.ui.home.HomeFragment;
import com.example.spiral_event_lottery_app.ui.notifications.NotificationFragment;
import com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment;
import com.example.event_creation.CreateEventActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
                ((MyEventsFragment) activeTab).refreshMyEvents();
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
        if (intent == null) return;
        String scannedId = intent.getStringExtra("SCAN_RESULT_ID");
        if (scannedId != null) {
            // US 01.06.01 - Open event details when QR is scanned
            Fragment detailsFragment = com.example.spiral_event_lottery_app.ui.details.EventDetailsFragment.Companion.newInstance(scannedId);
            
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, detailsFragment, "details_screen")
                    .addToBackStack("details")
                    .commit();
            
            // Clear the extra so it doesn't trigger again on rotation
            intent.removeExtra("SCAN_RESULT_ID");
        }
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
            ((MyEventsFragment) targetTab).refreshMyEvents();
        }
    }
}
