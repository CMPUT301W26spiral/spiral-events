package com.example.event_creation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity representing the Organizer Dashboard.
 * Currently redirects straight to CreateEventActivity as per requirement.
 */
public class OrganizerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Launch straight into create an event
        Intent intent = new Intent(this, CreateEventActivity.class);
        startActivity(intent);
        
        // Finish this activity so the user doesn't come back to a blank screen
        finish();
    }
}
