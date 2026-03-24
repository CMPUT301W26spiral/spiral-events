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
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.example.spiral_event_lottery_app.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * RegisterScreen handles the initial user profile creation.
 * It allows new users to enter their name, email, and optional phone number,
 * and choose a profile picture. The data is persisted to Firebase Firestore
 * and Storage, and a unique device ID is generated and saved locally.
 */
public class RegisterActivity extends AppCompatActivity {

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

    /**
     * Initializes the activity, sets up view bindings and click listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
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

    /**
     * Validates user input fields for correctness.
     * @param fullName The entered full name.
     * @param emailAddress The entered email address.
     * @param phoneNumber The entered phone number.
     * @return true if all mandatory inputs are valid, false otherwise.
     */
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

    /**
     * Orchestrates the saving process, uploading the photo if present then the Firestore document.
     * @param fullName User's full name.
     * @param emailAddress User's email.
     * @param phoneNumber User's phone.
     */
    private void saveProfileToFirebase(String fullName, String emailAddress, String phoneNumber) {
        confirmButton.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Use consistent hardware device ID
        String deviceId = DeviceIdProvider.getDeviceId(this);

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

    /**
     * Saves the final profile data to the 'users' collection in Firestore.
     * @param db Firestore instance.
     * @param uid Unique ID for the user document.
     * @param name User's name.
     * @param email User's email.
     * @param phone User's phone.
     * @param photoUrl URL of the uploaded photo, if any.
     */
    private void saveToFirestore(FirebaseFirestore db, String uid, String name, String email, String phone, String photoUrl) {
        // Use the User instance to save to Firestore
        User user = new User(
                uid,
                name,
                email,
                phone.isEmpty() ? null : phone,
                photoUrl,
                false, // isAdmin
                new ArrayList<String>() // eventList
        );

        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("is_registered", true).apply();

        db.collection("users").document(uid).set(user)
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
