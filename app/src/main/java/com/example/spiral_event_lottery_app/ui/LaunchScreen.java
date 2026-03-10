package com.example.spiral_event_lottery_app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.ui.register.RegisterScreen;
import com.google.firebase.FirebaseApp;

public class LaunchScreen extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.launch_screen);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        progressBar = findViewById(R.id.progressBar);
        getStartedButton = findViewById(R.id.get_started);

        checkUserRegistration();

        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchScreen.this, RegisterScreen.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkUserRegistration() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", null);

        if (deviceId != null) {
            // If already registered, go to the Welcome/Login screen
            Intent intent = new Intent(LaunchScreen.this, LoginScreen.class);
            startActivity(intent);
            finish();
        } else {
            // New user, show "Get Started"
            getStartedButton.setVisibility(View.VISIBLE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }
}