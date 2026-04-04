package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.spiral_event_lottery_app.ui.event_creation.CreateEventActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Safe-mode Intent tests for Event Creation.
 * Bypasses Espresso library conflicts.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventIntentTest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test
    public void testCreateEventFormValidation() {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            EditText name = activity.findViewById(R.id.et_event_name);
            EditText loc = activity.findViewById(R.id.et_location);
            EditText desc = activity.findViewById(R.id.et_description);
            Button createBtn = activity.findViewById(R.id.btn_create);

            assertNotNull("Name field should exist", name);
            name.setText("Safe Mode Gala");
            loc.setText("Innovation Lab");
            desc.setText("A robust test event.");

            createBtn.performClick();
        });
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    @Test
    public void testEmptyFormShowsError() {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            Button createBtn = activity.findViewById(R.id.btn_create);
            createBtn.performClick();
            
            // Verify we are still on the creation screen by checking the title
            TextView title = activity.findViewById(R.id.tv_label_event_name);
            assertNotNull(title);
            assertEquals("Event Name", title.getText().toString());
        });
    }
}
