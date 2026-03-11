package com.example.spiral_event_lottery_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.ui.LaunchScreen;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileScreen extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String uid;

    private ImageView profileImage;
    private ImageButton editPhotoButton;
    private TextView profileName, deviceIdText, fullNameText, emailText, phoneText;
    private EditText editFullName, editEmail, editPhone;

    private LinearLayout personalInfoViewGroup, personalInfoEditGroup, profileEditActions, notificationEditActions;
    private Button editProfileButton, cancelProfileEdit, saveProfileEdit, editNotificationsButton, cancelNotificationsEdit, saveNotificationsEdit, deleteProfileButton;
    private CheckBox whenChosenCheck, whenNotChosenCheck, organizersAdminsCheck;

    private Uri selectedImageUri;
    private String currentPhotoUrl = "";
    private String currentName = "", currentEmail = "", currentPhone = "";
    private boolean currentNotifyWhenChosen = true, currentNotifyWhenNotChosen = true, currentNotifyOrganizersAdmins = true;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImage.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_screen);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        uid = getOrCreateDeviceId();

        bindViews();
        setInitialUiState();
        setListeners();

        deviceIdText.setText(getString(R.string.device_id_label, uid));
        loadProfileFromFirebase();
    }

    private void bindViews() {
        profileImage = findViewById(R.id.profileImage);
        editPhotoButton = findViewById(R.id.editPhotoButton);
        profileName = findViewById(R.id.profileName);
        deviceIdText = findViewById(R.id.deviceIdText);
        fullNameText = findViewById(R.id.fullNameText);
        emailText = findViewById(R.id.emailText);
        phoneText = findViewById(R.id.phoneText);
        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        personalInfoViewGroup = findViewById(R.id.personalInfoViewGroup);
        personalInfoEditGroup = findViewById(R.id.personalInfoEditGroup);
        editProfileButton = findViewById(R.id.editProfileButton);
        profileEditActions = findViewById(R.id.profileEditActions);
        cancelProfileEdit = findViewById(R.id.cancelProfileEdit);
        saveProfileEdit = findViewById(R.id.saveProfileEdit);
        whenChosenCheck = findViewById(R.id.whenChosenCheck);
        whenNotChosenCheck = findViewById(R.id.whenNotChosenCheck);
        organizersAdminsCheck = findViewById(R.id.organizersAdminsCheck);
        editNotificationsButton = findViewById(R.id.editNotificationsButton);
        notificationEditActions = findViewById(R.id.notificationEditActions);
        cancelNotificationsEdit = findViewById(R.id.cancelNotificationsEdit);
        saveNotificationsEdit = findViewById(R.id.saveNotificationsEdit);
        deleteProfileButton = findViewById(R.id.deleteProfileButton);
    }

    private void setInitialUiState() {
        personalInfoViewGroup.setVisibility(View.VISIBLE);
        personalInfoEditGroup.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.VISIBLE);
        profileEditActions.setVisibility(View.GONE);
        setNotificationCheckboxesEnabled(false);
        editNotificationsButton.setVisibility(View.VISIBLE);
        notificationEditActions.setVisibility(View.GONE);
    }

    private void setListeners() {
        editPhotoButton.setOnClickListener(v -> pickImage.launch("image/*"));
        editProfileButton.setOnClickListener(v -> enterPersonalInfoEditMode());
        cancelProfileEdit.setOnClickListener(v -> cancelPersonalInfoEdit());
        saveProfileEdit.setOnClickListener(v -> savePersonalInfoToFirebase());
        editNotificationsButton.setOnClickListener(v -> enterNotificationsEditMode());
        cancelNotificationsEdit.setOnClickListener(v -> cancelNotificationsEdit());
        saveNotificationsEdit.setOnClickListener(v -> saveNotificationsToFirebase());
        deleteProfileButton.setOnClickListener(v -> showDeleteDialog());
    }

    private String getOrCreateDeviceId() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String id = prefs.getString("device_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }

    private void loadProfileFromFirebase() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::handleLoadedProfile)
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_to_load_profile, e.getMessage()), Toast.LENGTH_LONG).show());
    }

    private void handleLoadedProfile(DocumentSnapshot doc) {
        if (!doc.exists()) return;
        currentName = safeString(doc.getString("name"));
        currentEmail = safeString(doc.getString("email"));
        currentPhone = safeString(doc.getString("phone"));
        currentPhotoUrl = safeString(doc.getString("photoUrl"));
        currentNotifyWhenChosen = doc.getBoolean("notifyWhenChosen") != null && doc.getBoolean("notifyWhenChosen");
        currentNotifyWhenNotChosen = doc.getBoolean("notifyWhenNotChosen") != null && doc.getBoolean("notifyWhenNotChosen");
        currentNotifyOrganizersAdmins = doc.getBoolean("notifyOrganizersAdmins") != null && doc.getBoolean("notifyOrganizersAdmins");
        updateProfileViews();
        if (!currentPhotoUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
            Glide.with(this).load(currentPhotoUrl).into(profileImage);
        }
    }

    private void updateProfileViews() {
        profileName.setText(currentName.isEmpty() ? getString(R.string.unnamed_user) : currentName);
        fullNameText.setText(getString(R.string.full_name_label, currentName.isEmpty() ? "-" : currentName));
        emailText.setText(getString(R.string.email_label, currentEmail.isEmpty() ? "-" : currentEmail));
        phoneText.setText(getString(R.string.phone_label, currentPhone.isEmpty() ? "-" : currentPhone));
        editFullName.setText(currentName);
        editEmail.setText(currentEmail);
        editPhone.setText(currentPhone);
        whenChosenCheck.setChecked(currentNotifyWhenChosen);
        whenNotChosenCheck.setChecked(currentNotifyWhenNotChosen);
        organizersAdminsCheck.setChecked(currentNotifyOrganizersAdmins);
    }

    private void enterPersonalInfoEditMode() {
        personalInfoViewGroup.setVisibility(View.GONE);
        personalInfoEditGroup.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.GONE);
        profileEditActions.setVisibility(View.VISIBLE);
    }

    private void cancelPersonalInfoEdit() {
        personalInfoEditGroup.setVisibility(View.GONE);
        personalInfoViewGroup.setVisibility(View.VISIBLE);
        profileEditActions.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.VISIBLE);
        updateProfileViews();
        selectedImageUri = null;
        if (!currentPhotoUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
            Glide.with(this).load(currentPhotoUrl).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ellipse_1);
        }
    }

    private void savePersonalInfoToFirebase() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        if (!validatePersonalInfo(name, email, phone)) return;

        saveProfileEdit.setEnabled(false);
        cancelProfileEdit.setEnabled(false);

        if (selectedImageUri == null) {
            saveProfileDocument(name, email, phone, currentPhotoUrl);
            return;
        }

        StorageReference photoRef = storage.getReference().child("profile_photos/" + uid + ".jpg");
        photoRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw e != null ? e : new Exception("Upload failed");
                    }
                    return photoRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> saveProfileDocument(name, email, phone, uri.toString()))
                .addOnFailureListener(e -> {
                    saveProfileEdit.setEnabled(true);
                    cancelProfileEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.photo_upload_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileDocument(String name, String email, String phone, String photoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone.isEmpty() ? null : phone);
        updates.put("photoUrl", photoUrl.isEmpty() ? null : photoUrl);
        updates.put("deviceId", uid);
        updates.put("notifyWhenChosen", whenChosenCheck.isChecked());
        updates.put("notifyWhenNotChosen", whenNotChosenCheck.isChecked());
        updates.put("notifyOrganizersAdmins", organizersAdminsCheck.isChecked());

        db.collection("users").document(uid).set(updates)
                .addOnSuccessListener(unused -> {
                    currentName = name; currentEmail = email; currentPhone = phone; currentPhotoUrl = photoUrl == null ? "" : photoUrl;
                    selectedImageUri = null;
                    updateProfileViews();
                    setInitialUiState();
                    saveProfileEdit.setEnabled(true); cancelProfileEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    saveProfileEdit.setEnabled(true); cancelProfileEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.failed_to_save_profile, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validatePersonalInfo(String name, String email, String phone) {
        if (name.isEmpty()) { editFullName.setError("Required"); editFullName.requestFocus(); return false; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { editEmail.setError("Valid email required"); editEmail.requestFocus(); return false; }
        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) { editPhone.setError("Invalid phone"); editPhone.requestFocus(); return false; }
        return true;
    }

    private void enterNotificationsEditMode() {
        setNotificationCheckboxesEnabled(true);
        editNotificationsButton.setVisibility(View.GONE);
        notificationEditActions.setVisibility(View.VISIBLE);
    }

    private void cancelNotificationsEdit() {
        whenChosenCheck.setChecked(currentNotifyWhenChosen);
        whenNotChosenCheck.setChecked(currentNotifyWhenNotChosen);
        organizersAdminsCheck.setChecked(currentNotifyOrganizersAdmins);
        setNotificationCheckboxesEnabled(false);
        notificationEditActions.setVisibility(View.GONE);
        editNotificationsButton.setVisibility(View.VISIBLE);
    }

    private void saveNotificationsToFirebase() {
        saveNotificationsEdit.setEnabled(false); cancelNotificationsEdit.setEnabled(false);
        Map<String, Object> updates = new HashMap<>();
        updates.put("notifyWhenChosen", whenChosenCheck.isChecked());
        updates.put("notifyWhenNotChosen", whenNotChosenCheck.isChecked());
        updates.put("notifyOrganizersAdmins", organizersAdminsCheck.isChecked());

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    currentNotifyWhenChosen = whenChosenCheck.isChecked();
                    currentNotifyWhenNotChosen = whenNotChosenCheck.isChecked();
                    currentNotifyOrganizersAdmins = organizersAdminsCheck.isChecked();
                    setNotificationCheckboxesEnabled(false);
                    notificationEditActions.setVisibility(View.GONE);
                    editNotificationsButton.setVisibility(View.VISIBLE);
                    saveNotificationsEdit.setEnabled(true); cancelNotificationsEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.notification_settings_updated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    saveNotificationsEdit.setEnabled(true); cancelNotificationsEdit.setEnabled(true);
                    Toast.makeText(this, getString(R.string.failed_to_save_profile, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void setNotificationCheckboxesEnabled(boolean enabled) {
        whenChosenCheck.setEnabled(enabled); whenNotChosenCheck.setEnabled(enabled); organizersAdminsCheck.setEnabled(enabled);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this).setTitle("ALERT!").setMessage("ARE YOU SURE YOU WANT TO DELETE PROFILE?")
                .setPositiveButton("Confirm", (d, w) -> deleteProfile())
                .setNegativeButton("Cancel", null).show();
    }

    private void deleteProfile() {
        deleteProfileButton.setEnabled(false);
        storage.getReference().child("profile_photos/" + uid + ".jpg").delete()
                .addOnCompleteListener(task -> db.collection("users").document(uid).delete().addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.profile_deleted), Toast.LENGTH_SHORT).show();
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().remove("device_id").apply();
                    Intent intent = new Intent(this, LaunchScreen.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).addOnFailureListener(e -> deleteProfileButton.setEnabled(true)));
    }

    private String safeString(String value) { return value == null ? "" : value; }
}