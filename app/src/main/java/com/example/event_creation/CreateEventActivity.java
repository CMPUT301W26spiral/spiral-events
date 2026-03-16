package com.example.event_creation;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.model.Event;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for organizers to create and configure new lottery events.
 * Handles input validation, event creation, and navigation to QR code generation.
 */
public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName, etLocation, etInterests, etDescription, etMaxEntrants;
    private Spinner spinnerEventDay, spinnerEventMonth, spinnerEventYear;
    private Spinner spinnerEventStartHour, spinnerEventStartMin, spinnerEventEndHour, spinnerEventEndMin;
    private Spinner spinnerDrawDay, spinnerDrawMonth, spinnerDrawYear;
    private Spinner spinnerDrawStartHour, spinnerDrawStartMin, spinnerDrawEndHour, spinnerDrawEndMin;
    private Spinner spinnerGeolocation;
    private Button btnCreate;

    private ConstraintLayout postersContainer;
    private ImageView ivEventPoster;
    private LinearLayout llAddPosterPlaceholder;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivEventPoster.setImageURI(uri);
                    llAddPosterPlaceholder.setVisibility(View.GONE);
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception e) {}
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        initializeViews();
        setupSpinners();

        View.OnClickListener posterClickListener = v -> imagePickerLauncher.launch("image/*");
        postersContainer.setOnClickListener(posterClickListener);
        llAddPosterPlaceholder.setOnClickListener(posterClickListener);
        ivEventPoster.setOnClickListener(posterClickListener);

        btnCreate.setOnClickListener(v -> {
            if (validateForm()) {
                saveEvent();
            } else {
                // Toast already handled inside validateForm for specific cases
            }
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        postersContainer = findViewById(R.id.posters_container);
        ivEventPoster = findViewById(R.id.iv_event_poster);
        llAddPosterPlaceholder = findViewById(R.id.ll_add_poster_placeholder);

        etEventName = findViewById(R.id.et_event_name);
        etLocation = findViewById(R.id.et_location);
        etInterests = findViewById(R.id.et_interests);
        etDescription = findViewById(R.id.et_description);
        spinnerGeolocation = findViewById(R.id.spinner_geolocation);
        etMaxEntrants = findViewById(R.id.et_max_entrants);

        spinnerEventDay = findViewById(R.id.spinner_event_day);
        spinnerEventMonth = findViewById(R.id.spinner_event_month);
        spinnerEventYear = findViewById(R.id.spinner_event_year);

        spinnerEventStartHour = findViewById(R.id.spinner_event_start_hour);
        spinnerEventStartMin = findViewById(R.id.spinner_event_start_min);
        spinnerEventEndHour = findViewById(R.id.spinner_event_end_hour);
        spinnerEventEndMin = findViewById(R.id.spinner_event_end_min);

        spinnerDrawDay = findViewById(R.id.spinner_draw_day);
        spinnerDrawMonth = findViewById(R.id.spinner_draw_month);
        spinnerDrawYear = findViewById(R.id.spinner_draw_year);

        spinnerDrawStartHour = findViewById(R.id.spinner_draw_start_hour);
        spinnerDrawStartMin = findViewById(R.id.spinner_draw_start_min);
        spinnerDrawEndHour = findViewById(R.id.spinner_draw_end_hour);
        spinnerDrawEndMin = findViewById(R.id.spinner_draw_end_min);

        btnCreate = findViewById(R.id.btn_create);
    }

    private void setupSpinners() {
        populateSpinner(spinnerEventDay, 1, 31, 1, "DD");
        populateSpinner(spinnerDrawDay, 1, 31, 1, "DD");

        List<String> months = Arrays.asList("MM", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        setupCustomAdapter(spinnerEventMonth, months);
        setupCustomAdapter(spinnerDrawMonth, months);

        List<String> years = new ArrayList<>();
        years.add("YYYY");
        for (int i = 2026; i <= 2030; i++) years.add(String.valueOf(i));
        setupCustomAdapter(spinnerEventYear, years);
        setupCustomAdapter(spinnerDrawYear, years);

        populateSpinner(spinnerEventStartHour, 1, 24, 1, "HH");
        populateSpinner(spinnerEventEndHour, 1, 24, 1, "HH");
        populateSpinner(spinnerDrawStartHour, 1, 24, 1, "HH");
        populateSpinner(spinnerDrawEndHour, 1, 24, 1, "HH");

        // Minutes set to 15-minute increments
        populateSpinner(spinnerEventStartMin, 0, 59, 15, "mm");
        populateSpinner(spinnerEventEndMin, 0, 59, 15, "mm");
        populateSpinner(spinnerDrawStartMin, 0, 59, 15, "mm");
        populateSpinner(spinnerDrawEndMin, 0, 59, 15, "mm");

        List<String> geoOptions = Arrays.asList("Enable/Disable", "Enable", "Disable");
        setupCustomAdapter(spinnerGeolocation, geoOptions);
    }

    private void populateSpinner(Spinner spinner, int min, int max, int step, String placeholder) {
        List<String> items = new ArrayList<>();
        items.add(placeholder);
        for (int i = min; i <= max; i += step) {
            items.add(String.format(Locale.getDefault(), "%02d", i));
        }
        setupCustomAdapter(spinner, items);
    }

    private void setupCustomAdapter(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setPadding(32, 32, 32, 32);
                tv.setTextSize(16);

                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                    tv.setVisibility(View.GONE);
                    ViewGroup.LayoutParams params = tv.getLayoutParams();
                    params.height = 0;
                    tv.setLayoutParams(params);
                } else {
                    tv.setTextColor(Color.BLACK);
                    tv.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams params = tv.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    tv.setLayoutParams(params);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void saveEvent() {
        String eventName = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String interests = etInterests.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String geolocation = spinnerGeolocation.getSelectedItem().toString();

        String maxEntrantsStr = etMaxEntrants.getText().toString().trim();
        Integer maxEntrants = maxEntrantsStr.isEmpty() ? null : Integer.parseInt(maxEntrantsStr);

        // Convert month names back to numbers for the stored date string (e.g., Jan -> 01)
        String eventMonthStr = spinnerEventMonth.getSelectedItem().toString();
        String eventDayStr = spinnerEventDay.getSelectedItem().toString();
        String eventYearStr = spinnerEventYear.getSelectedItem().toString();
        String eventMonth = String.format(Locale.getDefault(), "%02d", spinnerEventMonth.getSelectedItemPosition());
        String eventDate = eventDayStr + "/" + eventMonth + "/" + eventYearStr;

        String eventStartTime = spinnerEventStartHour.getSelectedItem().toString() + ":" + spinnerEventStartMin.getSelectedItem().toString();
        String eventEndTime = spinnerEventEndHour.getSelectedItem().toString() + ":" + spinnerEventEndMin.getSelectedItem().toString();

        String drawMonth = String.format(Locale.getDefault(), "%02d", spinnerDrawMonth.getSelectedItemPosition());
        String drawDate = spinnerDrawDay.getSelectedItem().toString() + "/" + drawMonth + "/" + spinnerDrawYear.getSelectedItem().toString();

        String drawStartTime = spinnerDrawStartHour.getSelectedItem().toString() + ":" + spinnerDrawStartMin.getSelectedItem().toString();
        String drawEndTime = spinnerDrawEndHour.getSelectedItem().toString() + ":" + spinnerDrawEndMin.getSelectedItem().toString();

        // Format a human-readable timeText for the UI adapters to display
        String timeText = eventMonthStr + " " + eventDayStr + ", " + eventYearStr + " " + eventStartTime + "-" + eventEndTime;

        String posterUriString = (selectedImageUri != null) ? selectedImageUri.toString() : null;

        String organizerId = DeviceIdProvider.getDeviceId(this);
        Event newEvent = new Event(
                "", // id
                eventName,
                location, // locationName
                interests,
                description,
                geolocation,
                maxEntrants,
                eventDate,
                eventStartTime,
                eventEndTime,
                drawDate,
                drawStartTime,
                drawEndTime,
                posterUriString,
                getCurrentTimestamp(), // eventCreated
                timeText, // timeText (Now populated correctly)
                0L, // waitingCount
                organizerId // organizerId
        );

        EventManager.getInstance().addEvent(newEvent);
        Intent intent = new Intent(CreateEventActivity.this, QRCodeActivity.class);
        intent.putExtra("EVENT_NAME", eventName);
        intent.putExtra("EVENT_ID", newEvent.getId()); // Pass the generated ID
        startActivity(intent);
    }

    private boolean validateForm() {
        boolean isValid = true;
        isValid &= checkEmpty(etEventName);
        isValid &= checkEmpty(etLocation);
        isValid &= checkSpinnerSelected(spinnerEventDay);
        isValid &= checkSpinnerSelected(spinnerEventMonth);
        isValid &= checkSpinnerSelected(spinnerEventYear);
        isValid &= checkSpinnerSelected(spinnerEventStartHour);
        isValid &= checkSpinnerSelected(spinnerEventStartMin);
        isValid &= checkSpinnerSelected(spinnerEventEndHour);
        isValid &= checkSpinnerSelected(spinnerEventEndMin);
        isValid &= checkEmpty(etDescription);
        isValid &= checkSpinnerSelected(spinnerDrawDay);
        isValid &= checkSpinnerSelected(spinnerDrawMonth);
        isValid &= checkSpinnerSelected(spinnerDrawYear);
        isValid &= checkSpinnerSelected(spinnerDrawStartHour);
        isValid &= checkSpinnerSelected(spinnerDrawStartMin);
        isValid &= checkSpinnerSelected(spinnerDrawEndHour);
        isValid &= checkSpinnerSelected(spinnerDrawEndMin);
        isValid &= checkSpinnerSelected(spinnerGeolocation);

        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isDrawBeforeEvent()) {
            Toast.makeText(this, "Draw date and time must be before the event starts", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    /**
     * Compares the selected draw date and time with the event start date and time.
     * @return true if the draw occurs before the event starts, false otherwise.
     */
    private boolean isDrawBeforeEvent() {
        try {
            Calendar eventCal = Calendar.getInstance();
            eventCal.set(
                    Integer.parseInt(spinnerEventYear.getSelectedItem().toString()),
                    spinnerEventMonth.getSelectedItemPosition() - 1,
                    Integer.parseInt(spinnerEventDay.getSelectedItem().toString()),
                    Integer.parseInt(spinnerEventStartHour.getSelectedItem().toString()),
                    Integer.parseInt(spinnerEventStartMin.getSelectedItem().toString()),
                    0
            );
            eventCal.set(Calendar.MILLISECOND, 0);

            Calendar drawCal = Calendar.getInstance();
            drawCal.set(
                    Integer.parseInt(spinnerDrawYear.getSelectedItem().toString()),
                    spinnerDrawMonth.getSelectedItemPosition() - 1,
                    Integer.parseInt(spinnerDrawDay.getSelectedItem().toString()),
                    Integer.parseInt(spinnerDrawStartHour.getSelectedItem().toString()),
                    Integer.parseInt(spinnerDrawStartMin.getSelectedItem().toString()),
                    0
            );
            drawCal.set(Calendar.MILLISECOND, 0);

            return drawCal.before(eventCal);
        } catch (Exception e) {
            return false;
        }
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

    private boolean checkSpinnerSelected(Spinner spinner) {
        if (spinner.getSelectedItemPosition() == 0) {
            spinner.setBackgroundResource(R.drawable.edit_text_error_background);
            return false;
        } else {
            spinner.setBackgroundResource(R.drawable.edit_text_background);
            return true;
        }
    }
}
