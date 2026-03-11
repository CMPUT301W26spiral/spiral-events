package com.example.spiral_event_lottery_app;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment eventsFragment = new MyEventsFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private final Fragment notificationFragment = new NotificationFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeTab = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Initial setup
        fm.beginTransaction().add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, notificationFragment, "notifications").hide(notificationFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, eventsFragment, "events").hide(eventsFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, homeFragment, "home").commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment target = null;
            
            if (itemId == R.id.nav_home) target = homeFragment;
            else if (itemId == R.id.nav_events) target = eventsFragment;
            else if (itemId == R.id.nav_notifications) target = notificationFragment;
            else if (itemId == R.id.nav_account) target = profileFragment;

            if (target != null) {
                if (activeTab == target) {
                    // Double-tap on the same tab: reset to root list
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    switchTab(target);
                }
                return true;
            }
            return false;
        });
    }

    private void switchTab(Fragment targetTab) {
        FragmentTransaction ft = fm.beginTransaction();
        
        // Hide the current main tab
        ft.hide(activeTab);
        
        Fragment details = fm.findFragmentByTag("details_screen");

        // Logic: Moving between main tabs (Home <-> Events)
        if ((targetTab == homeFragment || targetTab == eventsFragment) && 
            (activeTab == homeFragment || activeTab == eventsFragment)) {
            // Standard behavior: Close details when switching major sections
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            details = null;
        } else if (details != null) {
            // Just hide the details if moving to/from a utility tab (Notifications/Account)
            ft.hide(details);
        }

        // Show the new tab
        ft.show(targetTab);

        // Restore details if we are returning to a tab that had one open
        if (details != null && details.isAdded() && (targetTab == homeFragment || targetTab == eventsFragment)) {
            // Only restore if we are coming from a utility tab
            if (activeTab == notificationFragment || activeTab == profileFragment) {
                ft.show(details);
            }
        }

        ft.commit();
        activeTab = targetTab;
    }
}
