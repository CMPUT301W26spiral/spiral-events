package com.example.spiral_event_lottery_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.spiral_event_lottery_app.MainActivity;
import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginActivity displays a personalized welcome message to returning users.
 * It retrieves the user's name and profile picture from Firebase Firestore
 * using the unique hardware device ID.
 */
public class LoginActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView profileImage;
    private TextView nameText;
    private TextView deviceIdText;
    private TextView footerIdText;
    private Button enterButton;

    /**
     * Initializes the welcome screen and triggers data loading.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_screen);

        welcomeText = findViewById(R.id.welcome_text);
        profileImage = findViewById(R.id.login_profile_image);
        nameText = findViewById(R.id.login_name);
        deviceIdText = findViewById(R.id.login_device_id);
        footerIdText = findViewById(R.id.footer_user_id);
        enterButton = findViewById(R.id.enter_button);

        loadUserData();

        enterButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    /**
     * Fetches user data from Firestore based on the hardware device ID.
     */
    private void loadUserData() {
        // Use hardware device ID
        String deviceId = DeviceIdProvider.getDeviceId(this);
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("is_registered", true).apply();

        deviceIdText.setText("Device ID: " + deviceId);
        footerIdText.setText("User ID: " + deviceId);

        if (deviceId != null && !deviceId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(deviceId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String photoUrl = documentSnapshot.getString("photoUrl");

                            welcomeText.setText("Welcome, " + (name != null ? name : "User") + "!");
                            nameText.setText(name);

                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this).load(photoUrl).into(profileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
