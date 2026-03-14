package com.example.spiral_event_lottery_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
 * Intent and UI tests for core user stories.
 * This class uses custom actions to handle BottomNavigationView visibility issues in Espresso.
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


}
