package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.view.View;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.spiral_event_lottery_app.ui.admin.AdminFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for AdminFragment.
 * Bypasses Espresso reflection crashes by using direct View assertions.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, AdminFragment.newInstance())
                    .commitNow();
        });
    }

    @Test
    public void testAdminPanelButtonsVisible() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull("Events button should exist", activity.findViewById(R.id.btn_admin_events));
            assertNotNull("Profiles button should exist", activity.findViewById(R.id.btn_admin_profiles));
            assertNotNull("Images button should exist", activity.findViewById(R.id.btn_admin_images));
            
            assertEquals("Events button should be visible", View.VISIBLE, activity.findViewById(R.id.btn_admin_events).getVisibility());
        });
    }

    @Test
    public void testAdminLogsAndCommentsTabs() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull("Notif Logs button should exist", activity.findViewById(R.id.btn_admin_notif_logs));
            assertNotNull("Comments button should exist", activity.findViewById(R.id.btn_admin_comments));
        });
    }
}
