package com.example.spiral_event_lottery_app.ui.register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
    private TextView choosePhotoText;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profilePic.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_screen);

        profilePic = findViewById(R.id.profile_circle);
        choosePhotoText = findViewById(R.id.edit_photo_text);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        confirmButton = findViewById(R.id.confirm);

        // Both the image and the text label trigger the picker
        profilePic.setOnClickListener(v -> pickImage.launch("image/*"));
        choosePhotoText.setOnClickListener(v -> pickImage.launch("image/*"));

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

    private boolean validateInputs(String fullName, String emailAddress, String phoneNumber) {
        if (fullName.isEmpty()) {
            nameInput.setError("Full name is required");
            nameInput.requestFocus();
            return false;
        }
        if (emailAddress.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            emailInput.setError("Valid email required");
            emailInput.requestFocus();
            return false;
        }
        if (!phoneNumber.isEmpty() && !Patterns.PHONE.matcher(phoneNumber).matches()) {
            phoneInput.setError("Valid phone required");
            phoneInput.requestFocus();
            return false;
        }
        return true;
    }

    private void saveProfileToFirebase(String fullName, String emailAddress, String phoneNumber) {
        confirmButton.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String deviceId = prefs.getString("device_id", UUID.randomUUID().toString());
        prefs.edit().putString("device_id", deviceId).apply();

        if (selectedImageUri == null) {
            saveToFirestore(db, deviceId, fullName, emailAddress, phoneNumber, null);
        } else {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_photos/" + deviceId + ".jpg");
            ref.putFile(selectedImageUri)
                    .continueWithTask(task -> ref.getDownloadUrl())
                    .addOnSuccessListener(uri -> saveToFirestore(db, deviceId, fullName, emailAddress, phoneNumber, uri.toString()))
                    .addOnFailureListener(e -> {
                        confirmButton.setEnabled(true);
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveToFirestore(FirebaseFirestore db, String uid, String name, String email, String phone, String photoUrl) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone.isEmpty() ? null : phone);
        profile.put("photoUrl", photoUrl);
        profile.put("deviceId", uid);

        db.collection("users").document(uid).set(profile)
                .addOnSuccessListener(unused -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    confirmButton.setEnabled(true);
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                });
    }
}