package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.spiral_event_lottery_app.ui.register.RegisterActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Safe-mode Intent test for Registration.
 * Bypasses Espresso reflection bugs by interacting with views directly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserFlowIntentTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void testRegistrationFlow() {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            EditText name = activity.findViewById(R.id.name_input);
            EditText email = activity.findViewById(R.id.email_input);
            EditText phone = activity.findViewById(R.id.phone_input);
            Button confirm = activity.findViewById(R.id.confirm);

            assertNotNull("Name input should exist", name);
            name.setText("Test User");
            email.setText("test@example.com");
            phone.setText("1234567890");

            assertTrue("Confirm button should be enabled", confirm.isEnabled());
            confirm.performClick();
        });
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }
}
