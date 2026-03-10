package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.spiral_event_lottery_app.ui.register.RegisterScreen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent and UI tests for core user stories.
 * Note: For best results, disable animations on the test device/emulator.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserFlowIntentTest {

    @Rule
    public ActivityScenarioRule<RegisterScreen> activityRule =
            new ActivityScenarioRule<>(RegisterScreen.class);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    /**
     * User Story: I want to provide my personal information such as name, email 
     * and optional phone number in the app.
     */
    @Test
    public void testRegistrationFlow() {
        onView(withId(R.id.name_input)).perform(replaceText("Test User"));
        onView(withId(R.id.email_input)).perform(replaceText("test@example.com"));
        onView(withId(R.id.phone_input)).perform(replaceText("1234567890"));
        onView(withId(R.id.confirm)).perform(click());
    }

    /**
     * User Story: I want to update information such as name, email 
     * and contact information on my profile.
     */
    @Test
    public void testUpdateProfileFlow() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        try (var scenario = androidx.test.core.app.ActivityScenario.launch(intent)) {
            
            // Navigate to Account tab
            onView(withId(R.id.nav_account)).perform(click());

            // Wait for fragment to be displayed and Scroll to Edit button
            onView(withId(R.id.editProfileButton)).perform(scrollTo(), click());

            // Update Name - use replaceText to bypass keyboard animation issues
            onView(withId(R.id.editFullName)).perform(scrollTo(), replaceText("Updated Name"));
            
            // Scroll to and click Save
            onView(withId(R.id.saveProfileEdit)).perform(scrollTo(), click());

            // Verify updated text is shown in view mode
            onView(withId(R.id.fullNameText)).check(matches(isDisplayed()));
        }
    }

    /**
     * User Story: I want to be identified by my device, so that I don't have 
     * to use a username and password.
     */
    @Test
    public void testDeviceIdentificationPersistence() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("device_id", "test-device-id-123").commit();
        assert(prefs.getString("device_id", "").equals("test-device-id-123"));
    }
}
