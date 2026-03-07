package com.example.spiral_event_lottery_app.ui.register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.MainActivity;
import com.example.spiral_event_lottery_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterScreen extends AppCompatActivity {

    private ImageView profilePic;
    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private Button confirmButton;
    private ImageButton editPhotoButton;

    private Uri selectedImageUri;

    // Lets the user pick an image from the phone
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profilePic.setImageURI(uri); // preview chosen image
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_screen);

        profilePic = findViewById(R.id.profile_circle);
        editPhotoButton = findViewById(R.id.edit_photo);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        confirmButton = findViewById(R.id.confirm);

        editPhotoButton.setOnClickListener(v -> pickImage.launch("image/*"));

        confirmButton.setOnClickListener(v -> {
            String fullName = nameInput.getText().toString().trim();
            String emailAddress = emailInput.getText().toString().trim();
            String phoneNumber = phoneInput.getText().toString().trim();

            if (!validateInputs(fullName, emailAddress, phoneNumber)) {
                return;
            }

            saveProfileToFirebase(fullName, emailAddress, phoneNumber);
        });
    }

    /**
     * Validates the text fields before saving.
     */
    private boolean validateInputs(String fullName, String emailAddress, String phoneNumber) {
        if (fullName.isEmpty()) {
            nameInput.setError("Full name is required");
            nameInput.requestFocus();
            return false;
        }

        if (emailAddress.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        if (!phoneNumber.isEmpty() && !Patterns.PHONE.matcher(phoneNumber).matches()) {
            phoneInput.setError("Enter a valid phone number");
            phoneInput.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Saves the user's profile to Firestore.
     * If a photo was selected, uploads it to Firebase Storage first.
     */
    private void saveProfileToFirebase(String fullName, String emailAddress, String phoneNumber) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Use SharedPreferences to keep a unique device-based id
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", null);

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }

        String uid = deviceId;

        // Disable button while saving to avoid duplicate taps
        confirmButton.setEnabled(false);

        if (selectedImageUri == null) {
            // Save only text fields
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", fullName);
            profile.put("email", emailAddress);
            profile.put("phone", phoneNumber.isEmpty() ? null : phoneNumber);
            profile.put("photoUrl", null);
            profile.put("deviceId", uid);

            db.collection("users")
                    .document(uid)
                    .set(profile)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    })
                    .addOnFailureListener(e -> {
                        confirmButton.setEnabled(true);
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

            return;
        }

        // Upload selected image to Firebase Storage
        StorageReference photoRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_photos/" + uid + ".jpg");

        photoRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return photoRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", fullName);
                    profile.put("email", emailAddress);
                    profile.put("phone", phoneNumber.isEmpty() ? null : phoneNumber);
                    profile.put("photoUrl", downloadUri.toString());
                    profile.put("deviceId", uid);

                    db.collection("users")
                            .document(uid)
                            .set(profile)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Profile and photo saved.", Toast.LENGTH_SHORT).show();
                                goToMainActivity();
                            })
                            .addOnFailureListener(e -> {
                                confirmButton.setEnabled(true);
                                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    confirmButton.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Opens the main screen after successful save.
     */
    private void goToMainActivity() {
        Intent intent = new Intent(RegisterScreen.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}