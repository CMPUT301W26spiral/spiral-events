package com.example.sprial_event_lottery_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etEventName, etLocation, etDescription;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        etEventName = findViewById(R.id.et_event_name);
        etLocation = findViewById(R.id.et_location);
        etDescription = findViewById(R.id.et_description);
        btnCreate = findViewById(R.id.btn_create);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventName = etEventName.getText().toString().trim();
                if (eventName.isEmpty()) {
                    Toast.makeText(CreateEventActivity.this, "Please enter an event name", Toast.LENGTH_SHORT).show();
                    return;
                }

                // In a real app, you'd save the event data to Firebase here.
                // For now, we just pass the name to the next activity.
                Intent intent = new Intent(CreateEventActivity.this, QRCodeActivity.class);
                intent.putExtra("EVENT_NAME", eventName);
                startActivity(intent);
            }
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }
}