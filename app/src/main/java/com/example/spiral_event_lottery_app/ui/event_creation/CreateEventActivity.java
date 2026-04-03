package com.example.spiral_event_lottery_app.ui.event_creation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.model.Event;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;
import com.example.spiral_event_lottery_app.ui.events.PosterAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for organizers to create and configure new lottery events.
 * Optimized for better performance with image compression and parallel uploads.
 */
public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName, etLocation, etInterests, etDescription, etMaxEntrants;
    private ChipGroup cgInterests;
    private Spinner spinnerEventDay, spinnerEventMonth, spinnerEventYear;
    private Spinner spinnerEventStartHour, spinnerEventStartMin, spinnerEventEndHour, spinnerEventEndMin;
    private Spinner spinnerDrawDay, spinnerDrawMonth, spinnerDrawYear;
    private Spinner spinnerDrawStartHour, spinnerDrawStartMin, spinnerDrawEndHour, spinnerDrawEndMin;
    private Spinner spinnerGeolocation, spinnerAccess;
    private Button btnCreate;
    private ProgressDialog progressDialog;

    private ViewPager2 posterViewPager;
    private TabLayout posterIndicator;
    private LinearLayout llAddPosterPlaceholder;
    private List<Uri> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    if (uris.size() > 3) {
                        showMaxImagesError();
                    } else {
                        selectedImageUris = new ArrayList<>(uris);
                        updatePosterPreview();
                    }
                }
            }
    );

    private void showMaxImagesError() {
        new AlertDialog.Builder(this)
                .setTitle("Too Many Posters")
                .setMessage("You can only select up to 3 images for the event posters.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updatePosterPreview() {
        if (selectedImageUris.isEmpty()) {
            llAddPosterPlaceholder.setVisibility(View.VISIBLE);
            posterViewPager.setVisibility(View.GONE);
            posterIndicator.setVisibility(View.GONE);
        } else {
            llAddPosterPlaceholder.setVisibility(View.GONE);
            posterViewPager.setVisibility(View.VISIBLE);
            posterIndicator.setVisibility(selectedImageUris.size() > 1 ? View.VISIBLE : View.GONE);

            List<String> uriStrings = new ArrayList<>();
            for (Uri uri : selectedImageUris) uriStrings.add(uri.toString());

            PosterAdapter adapter = new PosterAdapter(uriStrings);
            posterViewPager.setAdapter(adapter);

            new TabLayoutMediator(posterIndicator, posterViewPager, (tab, position) -> {}).attach();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        initializeViews();
        setupSpinners();
        setupInterestsTagSystem();

        View.OnClickListener posterClickListener = v -> pickImagesLauncher.launch("image/*");
        findViewById(R.id.posters_container).setOnClickListener(posterClickListener);
        llAddPosterPlaceholder.setOnClickListener(posterClickListener);

        posterViewPager.setClickable(false);
        posterViewPager.setFocusable(false);

        btnCreate.setOnClickListener(v -> {
            if (validateForm()) {
                uploadPostersAndSaveEvent();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        } else {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        posterViewPager = findViewById(R.id.iv_event_poster_pager);
        posterIndicator = findViewById(R.id.posterIndicator);
        llAddPosterPlaceholder = findViewById(R.id.ll_add_poster_placeholder);

        etEventName = findViewById(R.id.et_event_name);
        etLocation = findViewById(R.id.et_location);
        spinnerAccess = findViewById(R.id.spinner_access);
        cgInterests = findViewById(R.id.cg_interests);
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
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating event and uploading posters...");
        progressDialog.setCancelable(false);
    }

    private void setupInterestsTagSystem() {
        etInterests.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String interest = etInterests.getText().toString().trim();
                if (!interest.isEmpty()) {
                    addInterestTag(interest);
                    etInterests.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void addInterestTag(String interest) {
        Chip chip = new Chip(this);
        chip.setText(interest);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> cgInterests.removeView(chip));
        cgInterests.addView(chip);
    }

    private String getInterestsFromChips() {
        StringBuilder interests = new StringBuilder();
        for (int i = 0; i < cgInterests.getChildCount(); i++) {
            Chip chip = (Chip) cgInterests.getChildAt(i);
            if (interests.length() > 0) {
                interests.append(", ");
            }
            interests.append(chip.getText().toString());
        }
        return interests.toString();
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

        populateSpinner(spinnerEventStartMin, 0, 59, 15, "mm");
        populateSpinner(spinnerEventEndMin, 0, 59, 15, "mm");
        populateSpinner(spinnerDrawStartMin, 0, 59, 15, "mm");
        populateSpinner(spinnerDrawEndMin, 0, 59, 15, "mm");

        List<String> geoOptions = Arrays.asList("Enable/Disable", "Enable", "Disable");
        setupCustomAdapter(spinnerGeolocation, geoOptions);

        List<String> accessOptions = Arrays.asList("Select Access", "Public", "Private");
        setupCustomAdapter(spinnerAccess, accessOptions);
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

    /**
     * Optimized upload with image compression.
     */
    private void uploadPostersAndSaveEvent() {
        progressDialog.show();
        
        if (selectedImageUris.isEmpty()) {
            saveEvent(new ArrayList<>());
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        int totalToUpload = selectedImageUris.size();
        final int[] finishedCount = {0};

        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (Uri uri : selectedImageUris) {
            new Thread(() -> {
                try {
                    // Compress image before upload
                    byte[] data = compressImage(uri);
                    
                    StorageReference ref = storage.getReference().child("event_posters/" + UUID.randomUUID().toString() + ".jpg");
                    UploadTask uploadTask = ref.putBytes(data);
                    
                    uploadTask.continueWithTask(task -> ref.getDownloadUrl()).addOnSuccessListener(downloadUri -> {
                        synchronized (uploadedUrls) {
                            uploadedUrls.add(downloadUri.toString());
                            finishedCount[0]++;
                            if (finishedCount[0] == totalToUpload) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    saveEvent(uploadedUrls);
                                });
                            }
                        }
                    }).addOnFailureListener(e -> {
                        synchronized (uploadedUrls) {
                            finishedCount[0]++;
                            if (finishedCount[0] == totalToUpload) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    saveEvent(uploadedUrls);
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    synchronized (uploadedUrls) {
                        finishedCount[0]++;
                        if (finishedCount[0] == totalToUpload) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                saveEvent(uploadedUrls);
                            });
                        }
                    }
                }
            }).start();
        }
    }

    private byte[] compressImage(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        
        // Scale down if too large (e.g., max 1024px)
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float maxDimension = 1024f;
        if (width > maxDimension || height > maxDimension) {
            float scale = Math.min(maxDimension / width, maxDimension / height);
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // 70% quality
        return baos.toByteArray();
    }

    private void saveEvent(List<String> posterUrls) {
        String eventName = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        boolean isPublic = spinnerAccess.getSelectedItem().toString().equalsIgnoreCase("Public");
        String interests = getInterestsFromChips();
        String description = etDescription.getText().toString().trim();
        String geolocation = spinnerGeolocation.getSelectedItem().toString();

        String maxEntrantsStr = etMaxEntrants.getText().toString().trim();
        Integer maxEntrants = maxEntrantsStr.isEmpty() ? null : Integer.parseInt(maxEntrantsStr);

        String eventDayStr = spinnerEventDay.getSelectedItem().toString();
        String eventYearStr = spinnerEventYear.getSelectedItem().toString();
        String eventMonthNum = String.format(Locale.getDefault(), "%02d", spinnerEventMonth.getSelectedItemPosition());
        String eventDate = eventDayStr + "/" + eventMonthNum + "/" + eventYearStr;

        String eventStartTime = spinnerEventStartHour.getSelectedItem().toString() + ":" + spinnerEventStartMin.getSelectedItem().toString();
        String eventEndTime = spinnerEventEndHour.getSelectedItem().toString() + ":" + spinnerEventEndMin.getSelectedItem().toString();

        String drawMonthNum = String.format(Locale.getDefault(), "%02d", spinnerDrawMonth.getSelectedItemPosition());
        String drawDate = spinnerDrawDay.getSelectedItem().toString() + "/" + drawMonthNum + "/" + spinnerDrawYear.getSelectedItem().toString();

        String drawStartTime = spinnerDrawStartHour.getSelectedItem().toString() + ":" + spinnerDrawStartMin.getSelectedItem().toString();
        String drawEndTime = spinnerDrawEndHour.getSelectedItem().toString() + ":" + spinnerDrawEndMin.getSelectedItem().toString();

        String timeText = eventDate + " " + eventStartTime + "-" + eventEndTime;

        String organizerId = DeviceIdProvider.getDeviceId(this);

        Event newEvent = new Event(
                "", // id
                eventName,
                location, // locationName
                isPublic, // isPublic
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
                posterUrls.isEmpty() ? null : posterUrls.get(0), // primary poster
                posterUrls, // multi-posters list
                getCurrentTimestamp(), // eventCreated
                timeText, // timeText
                0L, // waitingCount
                organizerId // organizerId
        );

        EventManager.getInstance().addEvent(newEvent);

        if (isPublic) {
            Intent intent = new Intent(this, QRCodeActivity.class);
            intent.putExtra("EVENT_NAME", eventName);
            intent.putExtra("EVENT_ID", newEvent.getId());
            intent.putExtra("FROM_CREATION", true);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, PrivateEventSuccessActivity.class);
            intent.putExtra("EVENT_NAME", eventName);
            intent.putExtra("EVENT_ID", newEvent.getId());
            startActivity(intent);
        }
        finish();
    }

    private boolean validateForm() {
        boolean isValid = true;
        isValid &= checkEmpty(etEventName);
        isValid &= checkEmpty(etLocation);
        isValid &= checkSpinnerSelected(spinnerAccess);
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
