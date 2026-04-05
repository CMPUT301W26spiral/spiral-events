package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the Lottery/My Events navigation flow.
 * These tests verify that the user can navigate between screens 
 * and that the headers update correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LotteryInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Verifies that clicking the Events tab in the bottom navigation 
     * switches to the correct fragment and displays the "Current Events" header.
     */
    @Test
    public void testLotteryNavigationFlow() {
        try { Thread.sleep(2500); } catch (InterruptedException e) {}

        // 1. Navigate to Events tab in the bottom navigation bar
        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_events);
        });

        try { Thread.sleep(1500); } catch (InterruptedException e) {}

        // 2. Verify that the My Events page is displayed and has the correct header text
        activityRule.getScenario().onActivity(activity -> {
            TextView header = activity.findViewById(R.id.currentHeader);
            assertNotNull("Current Events header should exist", header);
            assertEquals("Current Events", header.getText().toString());
            assertEquals("Header should be visible", View.VISIBLE, header.getVisibility());
        });
    }
}
