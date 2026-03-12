package com.example.sprial_event_lottery_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for organizers to create and configure new lottery events.
 * Handles input validation, event creation, and navigation to QR code generation.
 */
public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName, etLocation, etInterests, etDescription, etGeolocation, etMaxEntrants;
    private EditText etEventDay, etEventMonth, etEventYear;
    private EditText etEventStartHour, etEventStartMin, etEventEndHour, etEventEndMin;
    private EditText etDrawDay, etDrawMonth, etDrawYear;
    private EditText etDrawStartHour, etDrawStartMin, etDrawEndHour, etDrawEndMin;
    private Button btnCreate;

    private FrameLayout postersContainer;
    private ImageView ivEventPoster;
    private LinearLayout llAddPosterPlaceholder;
    private Uri selectedImageUri;

    // Use GetContent for all image sources including Google Photos
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivEventPoster.setImageURI(selectedImageUri);
                    llAddPosterPlaceholder.setVisibility(View.GONE);
                    
                    // Try to keep permissions for long-term access if supported by the provider
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception e) {
                        // keep permissions might not be available for all URIs (e.g., from some cloud providers)
                    }
                }
            }
    );

    /**
     * Initializes the activity, views, and sets up click listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        initializeViews();
        setupInputRestrictions();

        // Listener for selecting an image
        View.OnClickListener posterClickListener = v -> {
            imagePickerLauncher.launch("image/*");
        };

        // Set listener on both the container and the placeholder to ensure it captures clicks
        postersContainer.setOnClickListener(posterClickListener);
        llAddPosterPlaceholder.setOnClickListener(posterClickListener);
        ivEventPoster.setOnClickListener(posterClickListener);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    saveEvent();
                } else {
                    Toast.makeText(CreateEventActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    /**
     * Collects data from input fields, creates a new Event object, 
     * saves it to the EventManager, and navigates to the QRCodeActivity.
     */
    private void saveEvent() {
        String eventName = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String interests = etInterests.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String geolocation = etGeolocation.getText().toString().trim();
        
        String maxEntrantsStr = etMaxEntrants.getText().toString().trim();
        Integer maxEntrants = maxEntrantsStr.isEmpty() ? null : Integer.parseInt(maxEntrantsStr);

        String eventDate = etEventDay.getText().toString() + "/" + etEventMonth.getText().toString() + "/" + etEventYear.getText().toString();
        String eventStartTime = etEventStartHour.getText().toString() + ":" + etEventStartMin.getText().toString();
        String eventEndTime = etEventEndHour.getText().toString() + ":" + etEventEndMin.getText().toString();

        String drawDate = etDrawDay.getText().toString() + "/" + etDrawMonth.getText().toString() + "/" + etDrawYear.getText().toString();
        String drawStartTime = etDrawStartHour.getText().toString() + ":" + etDrawStartMin.getText().toString();
        String drawEndTime = etDrawEndHour.getText().toString() + ":" + etDrawEndMin.getText().toString();

        String posterUriString = (selectedImageUri != null) ? selectedImageUri.toString() : null;

        Event newEvent = new Event(
                eventName, location, interests, description, geolocation, maxEntrants,
                eventDate, eventStartTime, eventEndTime,
                drawDate, drawStartTime, drawEndTime,
                posterUriString
        );

        // Store event in local list
        EventManager.getInstance().addEvent(newEvent);

        // Navigate to QR code generation
        Intent intent = new Intent(CreateEventActivity.this, QRCodeActivity.class);
        intent.putExtra("EVENT_NAME", eventName);
        startActivity(intent);
    }

    /**
     * Binds UI components from the layout XML to class variables.
     */
    private void initializeViews() {
        postersContainer = findViewById(R.id.posters_container);
        ivEventPoster = findViewById(R.id.iv_event_poster);
        llAddPosterPlaceholder = findViewById(R.id.ll_add_poster_placeholder);

        etEventName = findViewById(R.id.et_event_name);
        etLocation = findViewById(R.id.et_location);
        etInterests = findViewById(R.id.et_interests);
        etDescription = findViewById(R.id.et_description);
        etGeolocation = findViewById(R.id.et_geolocation);
        etMaxEntrants = findViewById(R.id.et_max_entrants);
        
        etEventDay = findViewById(R.id.et_event_day);
        etEventMonth = findViewById(R.id.et_event_month);
        etEventYear = findViewById(R.id.et_event_year);
        
        etEventStartHour = findViewById(R.id.et_event_start_hour);
        etEventStartMin = findViewById(R.id.et_event_start_min);
        etEventEndHour = findViewById(R.id.et_event_end_hour);
        etEventEndMin = findViewById(R.id.et_event_end_min);
        
        etDrawDay = findViewById(R.id.et_draw_day);
        etDrawMonth = findViewById(R.id.et_draw_month);
        etDrawYear = findViewById(R.id.et_draw_year);
        
        etDrawStartHour = findViewById(R.id.et_draw_start_hour);
        etDrawStartMin = findViewById(R.id.et_draw_start_min);
        etDrawEndHour = findViewById(R.id.et_draw_end_hour);
        etDrawEndMin = findViewById(R.id.et_draw_end_min);
        
        btnCreate = findViewById(R.id.btn_create);
    }

    /**
     * Sets up text watchers to enforce numeric ranges for date and time fields.
     */
    private void setupInputRestrictions() {
        addRangeWatcher(etEventDay, 1, 31);
        addRangeWatcher(etDrawDay, 1, 31);
        addRangeWatcher(etEventMonth, 1, 12);
        addRangeWatcher(etDrawMonth, 1, 12);
        addRangeWatcher(etEventStartHour, 0, 23);
        addRangeWatcher(etEventEndHour, 0, 23);
        addRangeWatcher(etDrawStartHour, 0, 23);
        addRangeWatcher(etDrawEndHour, 0, 23);
        addRangeWatcher(etEventStartMin, 0, 59);
        addRangeWatcher(etEventEndMin, 0, 59);
        addRangeWatcher(etDrawStartMin, 0, 59);
        addRangeWatcher(etDrawEndMin, 0, 59);
    }

    /**
     * Utility method to restrict EditText input within a specified range.
     */
    private void addRangeWatcher(EditText et, int min, int max) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        int val = Integer.parseInt(s.toString());
                        if (val < min || val > max) {
                            et.setText(s.toString().substring(0, s.length() - 1));
                            et.setSelection(et.getText().length());
                        }
                    } catch (NumberFormatException e) {
                        et.setText("");
                    }
                }
            }
        });
    }

    /**
     * Validates that all required fields are filled.
     */
    private boolean validateForm() {
        boolean isValid = true;
        isValid &= checkEmpty(etEventName);
        isValid &= checkEmpty(etLocation);
        isValid &= checkEmpty(etEventDay);
        isValid &= checkEmpty(etEventMonth);
        isValid &= checkEmpty(etEventYear);
        isValid &= checkEmpty(etEventStartHour);
        isValid &= checkEmpty(etEventStartMin);
        isValid &= checkEmpty(etEventEndHour);
        isValid &= checkEmpty(etEventEndMin);
        isValid &= checkEmpty(etDescription);
        isValid &= checkEmpty(etDrawDay);
        isValid &= checkEmpty(etDrawMonth);
        isValid &= checkEmpty(etDrawYear);
        isValid &= checkEmpty(etDrawStartHour);
        isValid &= checkEmpty(etDrawStartMin);
        isValid &= checkEmpty(etDrawEndHour);
        isValid &= checkEmpty(etDrawEndMin);
        isValid &= checkEmpty(etGeolocation);
        return isValid;
    }

    private boolean checkEmpty(EditText et) {
        if (et.getText().toString().trim().isEmpty()) {
            et.setBackgroundResource(R.drawable.edit_text_error_background);
            return false;
        } else {
            et.setBackgroundResource(R.drawable.edit_text_background);
            return true;
        }
    }
}
