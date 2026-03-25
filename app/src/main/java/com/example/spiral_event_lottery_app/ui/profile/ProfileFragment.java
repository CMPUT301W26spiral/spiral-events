package com.example.spiral_event_lottery_app.ui.profile;

import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileFragment provides the user interface for viewing and editing the user's profile.
 * It is hosted within the MainActivity as part of the bottom navigation.
 * This fragment manages personal information updates, notification preferences,
 * and profile deletion logic.
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
    private Button editProfileButton, cancelProfileEdit, saveProfileEdit, editNotificationsButton, cancelNotificationsEdit, saveNotificationsEdit, deleteProfileButton, changeInterestsButton;
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

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_screen, container, false);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        // Retrieve hardware ID from DeviceIdProvider
        uid = DeviceIdProvider.getDeviceId(requireContext());

        bindViews(view);
        setInitialUiState();
        setListeners();

        deviceIdText.setText(getString(R.string.device_id_label, uid));
        loadProfileFromFirebase();

        return view;
    }

    /**
     * Binds view variables to their respective XML IDs.
     * @param view The root view of the fragment.
     */
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
        changeInterestsButton = view.findViewById(R.id.changeInterestsButton);
    }

    /**
     * Sets the starting visibility and enabled states for the UI components.
     */
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

    /**
     * Attaches click listeners to buttons and interactive views.
     */
    private void setListeners() {
        editPhotoButton.setOnClickListener(v -> pickImage.launch("image/*"));
        editProfileButton.setOnClickListener(v -> enterPersonalInfoEditMode());
        cancelProfileEdit.setOnClickListener(v -> cancelPersonalInfoEdit());
        saveProfileEdit.setOnClickListener(v -> savePersonalInfoToFirebase());
        editNotificationsButton.setOnClickListener(v -> enterNotificationsEditMode());
        cancelNotificationsEdit.setOnClickListener(v -> cancelNotificationsEdit());
        saveNotificationsEdit.setOnClickListener(v -> saveNotificationsToFirebase());
        deleteProfileButton.setOnClickListener(v -> showDeleteDialog());
        logoutButton.setOnClickListener(v -> performLogout());
        changeInterestsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), InterestsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Handles the logout process by redirecting to the LaunchActivity and clearing task history.
     */
    private void performLogout() {
        // Clear local registration flag
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("is_registered", false).apply();

        Intent intent = new Intent(getActivity(), LaunchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    /**
     * Initiates a Firestore read to load the user's profile data.
     */
    private void loadProfileFromFirebase() {
        if (uid == null || uid.isEmpty()) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::handleLoadedProfile);
    }

    /**
     * Processes the loaded Firestore document and updates UI fields.
     * @param doc The DocumentSnapshot containing user profile data.
     */
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

    /**
     * Refreshes the text and check states of all profile views with current data.
     */
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

    /**
     * Transitions the Personal Information section into edit mode.
     */
    private void enterPersonalInfoEditMode() {
        personalInfoViewGroup.setVisibility(View.GONE);
        personalInfoEditGroup.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.GONE);
        profileEditActions.setVisibility(View.VISIBLE);
        editPhotoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Reverts the Personal Information section to view-only mode without saving.
     */
    private void cancelPersonalInfoEdit() {
        personalInfoEditGroup.setVisibility(View.GONE);
        personalInfoViewGroup.setVisibility(View.VISIBLE);
        profileEditActions.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.VISIBLE);
        editPhotoButton.setVisibility(View.GONE);
        updateProfileViews();
    }

    /**
     * Validates and saves personal information changes to Firebase.
     */
    private void savePersonalInfoToFirebase() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        if (!validatePersonalInfo(name, email, phone)) return;

        saveProfileEdit.setEnabled(false);
        cancelProfileEdit.setEnabled(false);

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
                        }
                    });
        }
    }

    /**
     * Writes the profile map to the 'users' Firestore collection.
     * @param name User's updated name.
     * @param email User's updated email.
     * @param photoUrl User's updated photo URL.
     */
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

        db.collection("users").document(uid).set(updates)
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        // Ensure local cache is updated
                        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("is_registered", true).apply();

                        currentName = name; currentEmail = email; currentPhone = phone; currentPhotoUrl = photoUrl == null ? "" : photoUrl;
                        updateProfileViews();
                        setInitialUiState();
                        saveProfileEdit.setEnabled(true); cancelProfileEdit.setEnabled(true);
                    }
                });
    }

    /**
     * Validates input strings for name and email formatting.
     * @param name Name string.
     * @param email Email string.
     * @param phone Phone string.
     * @return true if valid.
     */
    private boolean validatePersonalInfo(String name, String email, String phone) {
        if (name.isEmpty()) return false;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false;
        return true;
    }

    /**
     * Transitions the Notification preferences into edit mode.
     */
    private void enterNotificationsEditMode() {
        setNotificationCheckboxesEnabled(true);
        editNotificationsButton.setVisibility(View.GONE);
        notificationEditActions.setVisibility(View.VISIBLE);
    }

    /**
     * Reverts the Notification preferences to view-only mode without saving.
     */
    private void cancelNotificationsEdit() {
        whenChosenCheck.setChecked(currentNotifyWhenChosen);
        whenNotChosenCheck.setChecked(currentNotifyWhenNotChosen);
        organizersAdminsCheck.setChecked(currentNotifyOrganizersAdmins);
        setNotificationCheckboxesEnabled(false);
        notificationEditActions.setVisibility(View.GONE);
        editNotificationsButton.setVisibility(View.VISIBLE);
    }

    /**
     * Persists updated notification flags to Firestore.
     */
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
                    }
                });
    }

    /**
     * Enables or disables interaction with the notification checkboxes.
     * @param enabled true to enable, false to disable.
     */
    private void setNotificationCheckboxesEnabled(boolean enabled) {
        whenChosenCheck.setEnabled(enabled);
        whenNotChosenCheck.setEnabled(enabled);
        organizersAdminsCheck.setEnabled(enabled);
    }

    /**
     * Displays a confirmation dialog before permanent profile deletion.
     */
    private void showDeleteDialog() {
        new AlertDialog.Builder(requireContext()).setTitle("ALERT!").setMessage("DELETE PROFILE?")
                .setPositiveButton("Confirm", (d, w) -> deleteProfile()).setNegativeButton("Cancel", null).show();
    }

    /**
     * Deletes the user profile from Firestore and removes the local device ID and registration flag.
     */
    private void deleteProfile() {
        db.collection("users").document(uid).delete().addOnSuccessListener(unused -> {
            // Clear local flags
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove("device_id")
                    .putBoolean("is_registered", false)
                    .apply();
            performLogout();
        });
    }

    /**
     * Returns an empty string if the provided value is null.
     * @param value The value to check.
     * @return The original value or "".
     */
    private String safeString(String value) { return value == null ? "" : value; }
}
