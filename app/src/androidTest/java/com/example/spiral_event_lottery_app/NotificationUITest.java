package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the Notifications screen UI.
 * Verifies tab navigation and button presence.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testNotificationTabOpens() {
        // Wait for MainActivity to settle
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // 1. Click the Notifications icon in the bottom navbar
        onView(withId(R.id.nav_notifications)).perform(click());

        // 2. Check if the "Notifications" title area is displayed
        onView(withId(R.id.notifications_title)).check(matches(isDisplayed()));
    }

    @Test
    public void testClearAllButtonExists() {
        // 1. Go to Notifications tab
        onView(withId(R.id.nav_notifications)).perform(click());

        // 2. Check if the "Clear All" button is visible and clickable
        onView(withId(R.id.notification_clear_all_button)).check(matches(isDisplayed()));
        onView(withId(R.id.notification_clear_all_button)).perform(click());
        
        // Note: If list is empty, a Toast appears. If list has items, a Dialog appears.
        // Proving the button is clickable is sufficient for basic UI coverage.
    }
}
