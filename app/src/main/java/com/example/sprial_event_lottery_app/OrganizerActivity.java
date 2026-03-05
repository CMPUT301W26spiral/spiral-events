package com.example.sprial_event_lottery_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OrganizerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn_go_to_create).getParent() instanceof android.view.View ? (android.view.View) findViewById(R.id.btn_go_to_create).getParent() : null, (v, insets) -> {
            if (v == null) return insets;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_go_to_create).setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}