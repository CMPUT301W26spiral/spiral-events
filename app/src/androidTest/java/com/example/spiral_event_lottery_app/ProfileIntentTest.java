package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for Profile management.
 * These tests cover navigating to the profile, editing user information,
 * and toggling notification settings.
 * 
 * NOTE: Uses direct View access and manual thread sleeps to ensure stability 
 * without relying on Espresso's internal synchronization which can be flaky in some environments.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProfileIntentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Pre-configures shared preferences to simulate a registered user.
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_registered", true).commit();
    }

    /**
     * Verifies the full flow of switching to the Profile tab, entering edit mode,
     * updating the user's name, and saving the changes.
     */
    @Test
    public void testNavigateToProfileAndEdit() {
        try { Thread.sleep(2500); } catch (InterruptedException e) {}

        // 1. Switch to Profile tab via BottomNavigationView
        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_account);
        });

        try { Thread.sleep(1500); } catch (InterruptedException e) {}

        // 2. Verify visibility and click the Edit button
        activityRule.getScenario().onActivity(activity -> {
            TextView nameText = activity.findViewById(R.id.profileName);
            assertNotNull("Profile name view should exist", nameText);
            
            View editBtn = activity.findViewById(R.id.editProfileButton);
            assertNotNull("Edit button should exist", editBtn);
            editBtn.performClick();
        });

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // 3. Update the name in the input field and click Save
        activityRule.getScenario().onActivity(activity -> {
            EditText nameInput = activity.findViewById(R.id.editFullName);
            assertNotNull("Full name input should exist", nameInput);
            nameInput.setText("Safe Mode Name");
            
            View saveBtn = activity.findViewById(R.id.saveProfileEdit);
            assertNotNull("Save button should exist", saveBtn);
            saveBtn.performClick();
        });

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // 4. Verify the UI returns to the view mode (edit button visible again)
        activityRule.getScenario().onActivity(activity -> {
            View editBtn = activity.findViewById(R.id.editProfileButton);
            assertEquals("Should be back in view mode", View.VISIBLE, editBtn.getVisibility());
        });
    }

    /**
     * Verifies that the Notification settings edit mode can be toggled and cancelled.
     */
    @Test
    public void testNotificationsEditMode() {
        try { Thread.sleep(2500); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_account);
        });

        try { Thread.sleep(1500); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            View editNotifBtn = activity.findViewById(R.id.editNotificationsButton);
            assertNotNull("Edit Notifications button should exist", editNotifBtn);
            editNotifBtn.performClick();
            
            View saveNotifBtn = activity.findViewById(R.id.saveNotificationsEdit);
            assertEquals("Save button should be visible", View.VISIBLE, saveNotifBtn.getVisibility());
            
            View cancelBtn = activity.findViewById(R.id.cancelNotificationsEdit);
            cancelBtn.performClick();
        });
    }
}
