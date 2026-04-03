package com.example.spiral_event_lottery_app.ui.event_creation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.MainActivity;
import com.example.spiral_event_lottery_app.R;

/**
 * Activity shown after a private event is successfully created.
 */
public class PrivateEventSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_event_success);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        TextView tvEventName = findViewById(R.id.tv_event_name);
        if (eventName != null) {
            tvEventName.setText(eventName + "!");
        }

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button confirmButton = findViewById(R.id.btn_confirm);
        confirmButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
