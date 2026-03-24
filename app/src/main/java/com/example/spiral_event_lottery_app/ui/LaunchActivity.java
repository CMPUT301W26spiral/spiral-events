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
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.example.spiral_event_lottery_app.ui.register.RegisterActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LaunchActivity is the entry point of the application.
 * It serves as a splash screen and router, directing users to the LoginScreen
 * if they are already registered, or showing a "Get Started" button to lead
 * them to the registration process.
 */
public class LaunchActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button getStartedButton;

    /**
     * Called when the activity is first created.
     * Initializes Firebase and checks for existing user registration.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
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
            Intent intent = new Intent(LaunchActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Checks local cache first, then Firestore, to determine if the user
     * has previously registered. Redirects to LoginScreen if registered.
     */
    private void checkUserRegistration() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isRegisteredLocally = prefs.getBoolean("is_registered", false);

        if (isRegisteredLocally) {
            // CACHE HIT: Go straight to Login without waiting for network
            goToLogin();
        } else {
            // CACHE MISS: Perform one-time Firestore check
            performNetworkCheck();
        }
    }

    /**
     * Performs a one-time Firestore check if local registration status is unknown.
     */
    private void performNetworkCheck() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        getStartedButton.setVisibility(View.GONE);

        String deviceId = DeviceIdProvider.getDeviceId(this);

        FirebaseFirestore.getInstance().collection("users").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists in cloud, update cache and go to Login
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit().putBoolean("is_registered", true).apply();
                        goToLogin();
                    } else {
                        // User does not exist, force registration
                        showGetStarted();
                    }
                })
                .addOnFailureListener(e -> showGetStarted());
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
