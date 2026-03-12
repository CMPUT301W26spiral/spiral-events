package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the Notifications screen UI.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testNotificationTabOpens() {
        // 1. Click the Notifications icon in the bottom navbar
        onView(withId(R.id.nav_notifications)).perform(click());

        // We look for the one that has the specific ID of our title.
        onView(withId(R.id.notifications_title)).check(matches(withText("Notifications")));
        onView(withId(R.id.notifications_title)).check(matches(isDisplayed()));

        // 3. Check if the "Clear All" button is visible
        onView(withId(R.id.notification_clear_all_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testClearAllDialogAppears() {
        // 1. Go to Notifications tab
        onView(withId(R.id.nav_notifications)).perform(click());

        // 2. Click "Clear All"
        onView(withId(R.id.notification_clear_all_button)).perform(click());

        // 3. Check if the confirmation dialog appears
        onView(withText("Clear All Notifications")).check(matches(isDisplayed()));
    }
}
