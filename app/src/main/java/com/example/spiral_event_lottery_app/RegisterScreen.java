package com.example.sprial_event_lottery_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.MainActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class RegisterScreen extends AppCompatActivity {

    private ImageView profilePic;
    private Uri selectedImageUri; // holds chosen photo

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profilePic.setImageURI(selectedImageUri); // preview in the ImageView
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_screen);

        profilePic = findViewById(R.id.profile_pic);

        View editPhoto = findViewById(R.id.edit_photo);
        TextInputEditText name = findViewById(R.id.name_input);
        TextInputEditText email = findViewById(R.id.email_input);
        TextInputEditText phone_number = findViewById(R.id.phone_input);
        View confirm = findViewById(R.id.confirm);


        editPhoto.setOnClickListener(v -> {
            pickImage.launch("image/*");
        });

        confirm.setOnClickListener(v -> {
            String fullName = name.getText().toString().trim();
            String emailAddress = email.getText().toString().trim();
            String phoneNumber = phone_number.getText().toString().trim();

            // validation
            if (fullName.isEmpty() || emailAddress.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "Name, email, and phone number are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // save profile to firebase
//            saveProfileToFirebase(fullName, emailAddress, phoneNumber);

            // navigate to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            });
        }

        /*private void saveProfileToFirebase(String fullName, String emailAddress, String phoneNumber) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            // generate unique deviceid
            String deviceId = prefs.getString("device_id", null);

            if (deviceId == null) {
                deviceId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", deviceId).apply();
            }

            // check code here
            if (selectedImageUri == null) {
                // No photo: just save text fields
                Map<String, Object> profile = new HashMap<>();
                profile.put("name", fullName);
                profile.put("email", email);
                profile.put("phone", phone);
                profile.put("photoUrl", null);

                db.collection("users").document(uid)
                        .set(profile)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }

            // Photo chosen: upload to Firebase Storage
            StorageReference photoRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("profile_photos/" + uid + ".jpg");

            photoRef.putFile(selectedImageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return photoRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        // Save profile fields + photo URL to Firestore
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("name", fullName);
                        profile.put("email", email);
                        profile.put("phone", phone);
                        profile.put("photoUrl", downloadUri.toString());

                        db.collection("users").document(uid)
                                .set(profile)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Profile + photo saved.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

         */

}

