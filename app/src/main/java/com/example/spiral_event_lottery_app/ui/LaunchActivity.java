package com.example.spiral_event_lottery_app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.example.spiral_event_lottery_app.ui.register.RegisterActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class LaunchActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button getStartedButton;
    private boolean checkCompleted = false;

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

        // UI Fail-safe: If network takes more than 5 seconds, show the button anyway
        new Handler().postDelayed(() -> {
            if (!checkCompleted) {
                showGetStarted();
            }
        }, 5000);

        checkUserRegistration();

        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkUserRegistration() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isRegisteredLocally = prefs.getBoolean("is_registered", false);

        if (isRegisteredLocally) {
            checkCompleted = true;
            goToLogin();
        } else {
            performNetworkCheck();
        }
    }

    private void performNetworkCheck() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        getStartedButton.setVisibility(View.GONE);

        String deviceId = DeviceIdProvider.getDeviceId(this);

        FirebaseFirestore.getInstance().collection("users").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    checkCompleted = true;
                    if (documentSnapshot.exists()) {
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit().putBoolean("is_registered", true).apply();
                        goToLogin();
                    } else {
                        showGetStarted();
                    }
                })
                .addOnFailureListener(e -> {
                    checkCompleted = true;
                    showGetStarted();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(LaunchActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showGetStarted() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        getStartedButton.setVisibility(View.VISIBLE);
    }
}
