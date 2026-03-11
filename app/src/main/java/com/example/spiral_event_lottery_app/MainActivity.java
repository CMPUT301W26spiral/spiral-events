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

        // Setup tabs
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
                // If clicking the same tab, close any open details screen
                if (activeTab == target) {
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
        
        // Hide the current active tab
        ft.hide(activeTab);
        
        // Find and hide any EventDetails screen currently visible
        Fragment details = fm.findFragmentByTag("details_screen");
        if (details != null) {
            ft.hide(details);
        }

        // Show the new tab
        ft.show(targetTab);

        // If we are moving TO Home or Events, and there was a details screen, show it on top
        if (targetTab == homeFragment || targetTab == eventsFragment) {
            if (details != null && details.isAdded()) {
                ft.show(details);
            }
        }

        ft.commit();
        activeTab = targetTab;
    }
}
