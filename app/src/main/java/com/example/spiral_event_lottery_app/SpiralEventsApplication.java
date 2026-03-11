package com.example.spiral_event_lottery_app;

import android.app.Application;
import com.google.firebase.FirebaseApp;

/**
 * Custom Application class for global initialization.
 * Best practice for initializing Firebase once for the entire lifecycle.
 */
public class SpiralEventsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase automatically for all activities and fragments
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}
