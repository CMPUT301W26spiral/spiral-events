package com.example.sprial_event_lottery_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spiral_event_lottery_app.data.EventStore;
import com.example.spiral_event_lottery_app.ui.events.MyEventsFragment;
import com.example.spiral_event_lottery_app.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // default
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new HomeFragment())
                        .commit();
                return true;
            } else if (item.getItemId() == R.id.nav_events) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new MyEventsFragment())
                        .commit();
                return true;
            }
            return false;
        });
    }
}