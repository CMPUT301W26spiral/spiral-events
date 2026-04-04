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
 * Instrumented tests for the Notifications screen.
 * Uses direct View access to bypass Espresso library conflicts in the project environment.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testNotificationTabOpens() {
        // 1. Navigate to Notifications tab directly
        activityRule.getScenario().onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
            nav.setSelectedItemId(R.id.nav_notifications);
        });

        // 2. Wait for fragment transition
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // 3. Verify the title is displayed
        activityRule.getScenario().onActivity(activity -> {
            TextView title = activity.findViewById(R.id.notifications_title);
            assertNotNull("Notifications title view should exist", title);
            assertEquals("Notifications", title.getText().toString());
            assertEquals("Title should be visible", View.VISIBLE, title.getVisibility());
        });
    }

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
