package com.example.sprial_event_lottery_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;


public class RegisterScreen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_screen);

        View edit_photo = findViewById(R.id.edit_photo);
        TextInputEditText name = findViewById(R.id.name_input);
        TextInputEditText email = findViewById(R.id.email_input);
        TextInputEditText phone_number = findViewById(R.id.phone_input);
        View confirm = findViewById(R.id.confirm);


        edit_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

}

