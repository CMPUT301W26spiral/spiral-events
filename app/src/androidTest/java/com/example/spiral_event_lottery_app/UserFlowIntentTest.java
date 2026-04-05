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
 * Instrumented UI test for the User Registration flow.
 * Verifies that a new user can input their details and click the confirm button.
 * 
 * NOTE: Interacts with UI components directly to ensure stability across different 
 * test environments.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserFlowIntentTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    /**
     * Clear shared preferences before each test to ensure the registration screen 
     * is triggered as if it were a new user.
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    /**
     * Verifies that the registration form (Name, Email, Phone) can be filled 
     * and that the confirm button becomes enabled for submission.
     */
    @Test
    public void testRegistrationFlow() {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            EditText name = activity.findViewById(R.id.name_input);
            EditText email = activity.findViewById(R.id.email_input);
            EditText phone = activity.findViewById(R.id.phone_input);
            Button confirm = activity.findViewById(R.id.confirm);

            // Fill out the registration form
            assertNotNull("Name input should exist", name);
            name.setText("Test User");
            email.setText("test@example.com");
            phone.setText("1234567890");

            // Confirm that the button is enabled and can be clicked
            assertTrue("Confirm button should be enabled", confirm.isEnabled());
            confirm.performClick();
        });
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }
}
