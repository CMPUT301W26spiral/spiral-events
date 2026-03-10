package com.example.spiral_event_lottery_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.spiral_event_lottery_app.ui.ProfileFragment;
import com.example.spiral_event_lottery_app.ui.events.MyEventsFragment;
import com.example.spiral_event_lottery_app.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment eventsFragment = new MyEventsFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Pre-load fragments so they are ready instantly
        fm.beginTransaction().add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, eventsFragment, "events").hide(eventsFragment).commit();
        fm.beginTransaction().add(R.id.fragmentContainer, homeFragment, "home").commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fm.beginTransaction().hide(active).show(homeFragment).commit();
                active = homeFragment;
                return true;
            } else if (itemId == R.id.nav_events) {
                fm.beginTransaction().hide(active).show(eventsFragment).commit();
                active = eventsFragment;
                return true;
            } else if (itemId == R.id.nav_account) {
                fm.beginTransaction().hide(active).show(profileFragment).commit();
                active = profileFragment;
                return true;
            }
            return false;
        });
    }
}