package com.example.spiral_event_lottery_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

/**
 ** @author Abdul Haq Bin Abdul Rehman
 * In this activity we handle the QR scanner screen.
 * It uses CameraX to show the preview and ML kit from google to find the barcode.
 */
public class QR_scanner extends AppCompatActivity {
    private PreviewView preview_cam;
    private ProgressBar spinner_loads;
    //this is to stop scanning once we get it
    private boolean is_scan = true;

    //we asked the user if we can use the camera
    // handles asking the user for camera permission
    private final ActivityResultLauncher<String> request_permission_launcher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), is_granted -> {
                if (is_granted) {
                    open_camera();
                } else {
                    Toast.makeText(this, "Need camera permission to scan", Toast.LENGTH_SHORT).show();
                    finish(); // if said no
                }
            });
    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.qr_scanner);
        preview_cam = findViewById(R.id.camera_preview);
        spinner_loads = findViewById(R.id.loading_spinner);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            open_camera();
        } else {
            request_permission_launcher.launch(Manifest.permission.CAMERA);
        }
    }
    /**
     * This will set up the camera and binds it to the screen layout.
     */
    private void open_camera() {
        ListenableFuture<ProcessCameraProvider> camera_provider_future = ProcessCameraProvider.getInstance(this);

        camera_provider_future.addListener(() -> {
            try {
                ProcessCameraProvider camera_provider = camera_provider_future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(preview_cam.getSurfaceProvider());
                ImageAnalysis image_analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                image_analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::process_image);
                camera_provider.unbindAll();
                camera_provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, image_analysis);
            } catch (Exception e) {
                Log.e("QrScanner", "camera setup failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    /**
     * It looks at the camera frames to first see if there is a QR code there.
     * @param image_proxy the frame from the camera
     */
    private void process_image(@NonNull ImageProxy image_proxy) {
        if (!is_scan) {
            image_proxy.close();
            return;
        }
        @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
        android.media.Image media_image = image_proxy.getImage();

        if (media_image != null) {
            InputImage image = InputImage.fromMediaImage(media_image, image_proxy.getImageInfo().getRotationDegrees());
            BarcodeScanner scanner = BarcodeScanning.getClient();
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String raw_value = barcode.getRawValue();
                            if (raw_value != null && is_scan) {
                                is_scan = false; // we got one so we cna stop
                                handle_successful_scan(raw_value);
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> image_proxy.close());
        } else {
            image_proxy.close();
        }
    }

    /**
     * This will start when a QR code is found. It shows a loading spinner,
     * tells the user the ID found, and moves to the Organizer screen.
     * @param scanned_data it is the text which is the Event ID from the QR code
     */
    private void handle_successful_scan(String scanned_data) {
        // Shows the loading spinner
        spinner_loads.setVisibility(View.VISIBLE);
        // we show a quick message with the ID
        Toast.makeText(this, "Event ID: " + scanned_data, Toast.LENGTH_LONG).show();
        // now we navigate to Sean's screen
        Intent intent = new Intent(this, com.example.event_creation.OrganizerActivity.class);
        // now we pass the scanned ID so the screen can load the right event
        intent.putExtra("EVENT_ID", scanned_data);
        startActivity(intent);
        //now we close the scanner so it wont keep running in the background
        finish();
    }
}
