package com.example.spiral_event_lottery_app.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.example.spiral_event_lottery_app.ui.LaunchActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileFragment provides the user interface for viewing and editing the user's profile.
 * It is hosted within the MainActivity as part of the bottom navigation.
 */
public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String uid;

    private ImageView profileImage;
    private ImageButton editPhotoButton;
    private TextView profileName, deviceIdText, fullNameText, emailText, phoneText;
    private EditText editFullName, editEmail, editPhone;

    private LinearLayout personalInfoViewGroup, personalInfoEditGroup, profileEditActions, notificationEditActions;
    private Button editProfileButton, cancelProfileEdit, saveProfileEdit, editNotificationsButton, cancelNotificationsEdit, saveNotificationsEdit, deleteProfileButton;
    private Button logoutButton;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_screen, container, false);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        uid = DeviceIdProvider.getDeviceId(requireContext());

        bindViews(view);
        setInitialUiState();
        setListeners();

        deviceIdText.setText(getString(R.string.device_id_label, uid));
        loadProfileFromFirebase();

        return view;
    }

    private void bindViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        editPhotoButton = view.findViewById(R.id.editPhotoButton);
        profileName = view.findViewById(R.id.profileName);
        deviceIdText = view.findViewById(R.id.deviceIdText);
        fullNameText = view.findViewById(R.id.fullNameText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        editFullName = view.findViewById(R.id.editFullName);
        editEmail = view.findViewById(R.id.editEmail);
        editPhone = view.findViewById(R.id.editPhone);
        personalInfoViewGroup = view.findViewById(R.id.personalInfoViewGroup);
        personalInfoEditGroup = view.findViewById(R.id.personalInfoEditGroup);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        profileEditActions = view.findViewById(R.id.profileEditActions);
        cancelProfileEdit = view.findViewById(R.id.cancelProfileEdit);
        saveProfileEdit = view.findViewById(R.id.saveProfileEdit);
        whenChosenCheck = view.findViewById(R.id.whenChosenCheck);
        whenNotChosenCheck = view.findViewById(R.id.whenNotChosenCheck);
        organizersAdminsCheck = view.findViewById(R.id.organizersAdminsCheck);
        editNotificationsButton = view.findViewById(R.id.editNotificationsButton);
        notificationEditActions = view.findViewById(R.id.notificationEditActions);
        cancelNotificationsEdit = view.findViewById(R.id.cancelNotificationsEdit);
        saveNotificationsEdit = view.findViewById(R.id.saveNotificationsEdit);
        deleteProfileButton = view.findViewById(R.id.deleteProfileButton);
        logoutButton = view.findViewById(R.id.logoutButton);
    }

    private void setInitialUiState() {
        personalInfoViewGroup.setVisibility(View.VISIBLE);
        personalInfoEditGroup.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.VISIBLE);
        profileEditActions.setVisibility(View.GONE);
        editPhotoButton.setVisibility(View.GONE);
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
        logoutButton.setOnClickListener(v -> {
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_registered", false).apply();
            performLogout();
        });
    }

    private void performLogout() {
        Intent intent = new Intent(getActivity(), LaunchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private void loadProfileFromFirebase() {
        if (uid == null || uid.isEmpty()) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::handleLoadedProfile);
    }

    private void handleLoadedProfile(DocumentSnapshot doc) {
        if (!isAdded()) return;
        if (doc.exists()) {
            currentName = safeString(doc.getString("name"));
            currentEmail = safeString(doc.getString("email"));
            currentPhone = safeString(doc.getString("phone"));
            currentPhotoUrl = safeString(doc.getString("photoUrl"));
            
            Boolean notifyChosen = doc.getBoolean("notifyWhenChosen");
            currentNotifyWhenChosen = notifyChosen != null ? notifyChosen : true;
            Boolean notifyNotChosen = doc.getBoolean("notifyWhenNotChosen");
            currentNotifyWhenNotChosen = notifyNotChosen != null ? notifyNotChosen : true;
            Boolean notifyOrganizers = doc.getBoolean("notifyOrganizersAdmins");
            currentNotifyOrganizersAdmins = notifyOrganizers != null ? notifyOrganizers : true;
            
            updateProfileViews();
            if (!currentPhotoUrl.isEmpty()) {
                Glide.with(this).load(currentPhotoUrl).into(profileImage);
            }
        }
    }

    private void updateProfileViews() {
        if (!isAdded()) return;
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
        editPhotoButton.setVisibility(View.VISIBLE);
    }

    private void cancelPersonalInfoEdit() {
        personalInfoEditGroup.setVisibility(View.GONE);
        personalInfoViewGroup.setVisibility(View.VISIBLE);
        profileEditActions.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.VISIBLE);
        editPhotoButton.setVisibility(View.GONE);
        updateProfileViews();
    }

    private void savePersonalInfoToFirebase() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        if (!validatePersonalInfo(name, email, phone)) return;

        saveProfileEdit.setEnabled(false);
        cancelProfileEdit.setEnabled(false);
        Toast.makeText(getContext(), "Saving profile...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri == null) {
            saveProfileDocument(name, email, currentPhotoUrl);
        } else {
            StorageReference ref = storage.getReference().child("profile_photos/" + uid + ".jpg");
            ref.putFile(selectedImageUri)
                    .continueWithTask(task -> ref.getDownloadUrl())
                    .addOnSuccessListener(uri -> saveProfileDocument(name, email, uri.toString()))
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            saveProfileEdit.setEnabled(true);
                            cancelProfileEdit.setEnabled(true);
                            Toast.makeText(getContext(), "Photo upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveProfileDocument(String name, String email, String photoUrl) {
        String phone = editPhone.getText().toString().trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone.isEmpty() ? null : phone);
        updates.put("photoUrl", (photoUrl == null || photoUrl.isEmpty()) ? null : photoUrl);
        updates.put("deviceId", uid);
        updates.put("notifyWhenChosen", whenChosenCheck.isChecked());
        updates.put("notifyWhenNotChosen", whenNotChosenCheck.isChecked());
        updates.put("notifyOrganizersAdmins", organizersAdminsCheck.isChecked());

        db.collection("users").document(uid).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        currentName = name; currentEmail = email; currentPhone = phone; currentPhotoUrl = photoUrl == null ? "" : photoUrl;
                        updateProfileViews();
                        setInitialUiState();
                        saveProfileEdit.setEnabled(true); cancelProfileEdit.setEnabled(true);
                        Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        saveProfileEdit.setEnabled(true); cancelProfileEdit.setEnabled(true);
                        Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validatePersonalInfo(String name, String email, String phone) {
        if (name.isEmpty()) {
            editFullName.setError("Name required");
            Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Valid email required");
            Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
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
                    if (isAdded()) {
                        currentNotifyWhenChosen = whenChosenCheck.isChecked();
                        currentNotifyWhenNotChosen = whenNotChosenCheck.isChecked();
                        currentNotifyOrganizersAdmins = organizersAdminsCheck.isChecked();
                        setNotificationCheckboxesEnabled(false);
                        notificationEditActions.setVisibility(View.GONE);
                        editNotificationsButton.setVisibility(View.VISIBLE);
                        saveNotificationsEdit.setEnabled(true); cancelNotificationsEdit.setEnabled(true);
                        Toast.makeText(getContext(), "Notifications updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        saveNotificationsEdit.setEnabled(true); cancelNotificationsEdit.setEnabled(true);
                        Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setNotificationCheckboxesEnabled(boolean enabled) {
        whenChosenCheck.setEnabled(enabled);
        whenNotChosenCheck.setEnabled(enabled);
        organizersAdminsCheck.setEnabled(enabled);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(requireContext()).setTitle("ALERT!").setMessage("DELETE PROFILE?")
                .setPositiveButton("Confirm", (d, w) -> deleteProfile()).setNegativeButton("Cancel", null).show();
    }

    private void deleteProfile() {
        db.collection("users").document(uid).delete().addOnSuccessListener(unused -> {
            SharedPreferences.Editor editor = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit();
            editor.remove("is_registered");
            editor.apply();
            Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
            performLogout();
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String safeString(String value) { return value == null ? "" : value; }
}
