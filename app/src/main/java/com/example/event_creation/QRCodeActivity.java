package com.example.event_creation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
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
 * Activity that displays a generated QR code for a newly created event.
 * Organizers can save this QR code or share it with potential entrants.
 * The QR code contains the Firestore Event ID.
 */
public class QRCodeActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvSuccessMsg;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        ivQRCode = findViewById(R.id.iv_qr_code);
        tvSuccessMsg = findViewById(R.id.tv_success_msg);
        btnConfirm = findViewById(R.id.btn_confirm);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        String eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventName != null) {
            tvSuccessMsg.setText(getString(R.string.successfully_created, eventName));
        }

        if (eventId != null) {
            generateQRCode(eventId);
        } else {
            Toast.makeText(this, "Error: No event ID received", Toast.LENGTH_SHORT).show();
        }

        btnConfirm.setOnClickListener(v -> {
            // Navigate back to the home screen (MainActivity)
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.ll_save_qr).setOnClickListener(v -> {
            Toast.makeText(this, "QR Code saved to gallery (simulated)", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Generates a QR code bitmap from the provided text (Event ID).
     * @param eventId The Firestore document ID for the event.
     */
    private void generateQRCode(String eventId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // US 01.06.01 - Encoding the event ID so entrants can scan to view details
            Bitmap bitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 512, 512);
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
