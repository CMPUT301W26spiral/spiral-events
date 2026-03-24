package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Robust Instrumented test for the Lottery navigation flow.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LotteryInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testLotteryNavigationFlow() {
        // Wait for MainActivity to settle
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // 1. Navigate to the Events tab using the navbar ID
        onView(withId(R.id.nav_events)).perform(click());
        
        // 2. Verify we are on the My Events page by checking for the exact XML header ID
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // We check for the 'Current Events' header ID from your layout file
        onView(withId(R.id.currentHeader)).check(matches(isDisplayed()));
        onView(withId(R.id.currentHeader)).check(matches(withText("Current Events")));
    }
}
