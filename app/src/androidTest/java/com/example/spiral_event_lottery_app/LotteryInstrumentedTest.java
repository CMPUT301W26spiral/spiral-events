package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.SharedPreferences;

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
 * Instrumented test for the Lottery Draw system.
 * This test verifies that organizer events appear alongside joined events
 * and that the draw flow works for 'Family Swimming Lessons'.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LotteryInstrumentedTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    /**
     * Verifies the full UI flow of an organizer performing a draw for 'Family Swimming Lessons' 
     * on the unified My Events screen.
     */
    @Test
    public void testPerformLotteryOnEvent1() {
        // 1. Register a test user
        onView(withId(R.id.name_input)).perform(replaceText("Lottery Admin"));
        onView(withId(R.id.email_input)).perform(replaceText("admin@example.com"));
        onView(withId(R.id.confirm)).perform(click());

        // Wait for Firebase to save and transition to MainActivity
        try { Thread.sleep(8000); } catch (InterruptedException e) {}

        // 2. Navigate to the Events tab
        onView(withId(R.id.nav_events)).perform(click());

        // 3. Wait for the event to appear in the "Organizer Events" section
        try { Thread.sleep(4000); } catch (InterruptedException e) { e.printStackTrace(); }

        // 4. Verify event name is visible (matches the name in Firestore)
        onView(withText("Family Swimming Lessons")).check(matches(isDisplayed()));
        
        // 5. Click its details button
        onView(allOf(withId(R.id.detailsButton), withText("Details"))).perform(click());

        // 6. Navigate to the Draw screen
        onView(withId(R.id.drawButton)).perform(click());

        // 7. Input a limit of 3 and perform the draw
        onView(withId(R.id.entrantLimitInput)).perform(replaceText("3"));
        onView(withId(R.id.doDrawButton)).perform(click());

        // 8. Verify the Draw Results fragment is displayed
        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
        onView(withText("The Winners!")).check(matches(isDisplayed()));
    }
}
