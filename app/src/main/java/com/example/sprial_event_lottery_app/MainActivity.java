package com.example.sprial_event_lottery_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity is no longer the entry point of the app.
 * OrganizerActivity is configured as the launcher in the AndroidManifest.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity is currently not in use.
    }
}
