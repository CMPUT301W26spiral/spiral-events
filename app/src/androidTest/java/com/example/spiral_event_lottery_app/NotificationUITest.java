package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the Notifications screen.
 * These tests verify that the notification center is accessible and contains 
 * the necessary management controls.
 * 
 * NOTE: Uses direct View access to avoid potential library conflicts with Espresso 
 * in certain project environments.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Verifies that the Notifications tab can be opened via the bottom navigation
     * and that the screen title is correctly displayed.
     */
    @Test
    public void testNotificationTabOpens() {
        // 1. Navigate to Notifications tab directly
        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_notifications);
        });

        // 2. Wait for fragment transition
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // 3. Verify the title "Notifications" is displayed
        activityRule.getScenario().onActivity(activity -> {
            TextView title = activity.findViewById(R.id.notifications_title);
            assertNotNull("Notifications title view should exist", title);
            assertEquals("Notifications", title.getText().toString());
            assertEquals("Title should be visible", View.VISIBLE, title.getVisibility());
        });
    }

    /**
     * Verifies that the "Clear All" button exists on the notifications screen.
     */
    @Test
    public void testClearAllButtonExists() {
        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_notifications);
        });

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        activityRule.getScenario().onActivity(activity -> {
            View clearBtn = activity.findViewById(R.id.notification_clear_all_button);
            assertNotNull("Clear All button should exist", clearBtn);
            assertEquals("Button should be visible", View.VISIBLE, clearBtn.getVisibility());
        });
    }
}
