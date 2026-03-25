package com.example.event_creation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.MainActivity;
import com.example.spiral_event_lottery_app.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Activity that displays a generated QR code for an event.
 * Used both after event creation and when viewing QR from event details.
 */
public class QRCodeActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvEventName;
    private Button btnDone;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using the new layout requested for viewing event QR codes
        setContentView(R.layout.qr_code_event);

        ivQRCode = findViewById(R.id.iv_qr_code);
        tvEventName = findViewById(R.id.tv_event_name);
        btnDone = findViewById(R.id.btn_done);
        btnBack = findViewById(R.id.btn_back);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        String eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventName != null) {
            tvEventName.setText(eventName);
        }

        if (eventId != null) {
            generateQRCode(eventId);
        } else {
            Toast.makeText(this, "Error: No event ID received", Toast.LENGTH_SHORT).show();
        }

        // Back button finishes the activity
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Done button behavior:
        // If coming from creation, go to MainActivity. If just viewing, finish.
        btnDone.setOnClickListener(v -> {
            boolean fromCreation = getIntent().getBooleanExtra("FROM_CREATION", false);
            if (fromCreation) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            finish();
        });
    }

    /**
     * Generates a QR code bitmap from the provided text (Event ID).
     * @param eventId The Firestore document ID for the event.
     */
    private void generateQRCode(String eventId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 512, 512);
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
