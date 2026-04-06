package com.example.spiral_event_lottery_app.ui.event_creation;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.spiral_event_lottery_app.MainActivity;
import com.example.spiral_event_lottery_app.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Activity that displays a generated QR code for an event.
 * Used both after event creation and when viewing QR from event details.
 */
public class QRCodeActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvEventName;
    private Button btnDone;
    private View btnShareQR; // Can be MaterialButton or LinearLayout
    private ImageButton btnBack;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Determine which layout is being used and set it up
        // This activity seems to be used with two different layouts depending on the flow
        boolean fromCreation = getIntent().getBooleanExtra("FROM_CREATION", false);
        
        if (fromCreation) {
            setContentView(R.layout.activity_qr_code);
            btnConfirm = findViewById(R.id.btn_confirm);
            btnShareQR = findViewById(R.id.ll_share_qr);
            
            if (btnConfirm != null) {
                btnConfirm.setOnClickListener(v -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });
            }
        } else {
            setContentView(R.layout.qr_code_event);
            btnDone = findViewById(R.id.btn_done);
            btnShareQR = findViewById(R.id.btn_share_qr);
            btnBack = findViewById(R.id.btn_back);

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            if (btnDone != null) {
                btnDone.setOnClickListener(v -> finish());
            }
        }

        ivQRCode = findViewById(R.id.iv_qr_code);
        tvEventName = findViewById(R.id.tv_event_name);
        TextView tvSuccessMsg = findViewById(R.id.tv_success_msg);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        String eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventName != null) {
            if (tvEventName != null) {
                tvEventName.setText(eventName);
            } else if (tvSuccessMsg != null) {
                // If tv_event_name doesn't exist (like in activity_qr_code.xml),
                // use the success message to show the event name.
                tvSuccessMsg.setText("Successfully Created: " + eventName);
            }
        }

        if (eventId != null) {
            generateQRCode(eventId);
        } else {
            Toast.makeText(this, "Error: No event ID received", Toast.LENGTH_SHORT).show();
        }

        if (btnShareQR != null) {
            btnShareQR.setOnClickListener(v -> shareQRCode());
        }
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

    /**
     * Shares the generated QR code image.
     * This allows users to send it to other apps or add it to Google Photos.
     */
    private void shareQRCode() {
        if (ivQRCode.getDrawable() == null) {
            Toast.makeText(this, "No QR code to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap;
        if (ivQRCode.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) ivQRCode.getDrawable()).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(ivQRCode.getWidth(), ivQRCode.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            ivQRCode.draw(canvas);
        }

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "event_qr.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/png");
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
