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
        if (eventName != null) {
            tvSuccessMsg.setText(getString(R.string.successfully_created, eventName));
            generateQRCode(eventName);
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

    private void generateQRCode(String text) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // In future app development, this would be a link to the event in the app
            String qrContent = "spiral-events://event/" + text.replaceAll("\\s+", "_").toLowerCase();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 512, 512);
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
