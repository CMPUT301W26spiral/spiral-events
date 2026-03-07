package com.example.spiral_event_lottery_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spiral_event_lottery_app.R;
import com.example.spiral_event_lottery_app.ui.register.RegisterScreen;


public class LaunchScreen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.launch_screen);

        Button getStartedButton = findViewById(R.id.get_started);
        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchScreen.this, RegisterScreen.class);
            startActivity(intent);
            finish();
        });
    }

}

