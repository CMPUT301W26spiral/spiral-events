package com.example.spiral_event_lottery_app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginScreen displays a personalized welcome message to returning users.
 * It retrieves the user's name and profile picture from Firebase Firestore
 * using the unique device ID stored in SharedPreferences.
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
     * Fetches user data from Firestore based on the stored device ID.
     * Updates the UI with the user's name and photo upon success.
     */
    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", "");

        deviceIdText.setText("Device ID: " + deviceId);
        footerIdText.setText("User ID: " + deviceId);

        if (!deviceId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(deviceId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String photoUrl = documentSnapshot.getString("photoUrl");

                            welcomeText.setText("Welcome, " + name + "!");
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