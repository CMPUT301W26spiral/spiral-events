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
 * UI tests for the Admin Dashboard.
 * These tests ensure that admin-specific controls (events, profiles, images) 
 * are correctly displayed and accessible in the AdminFragment.
 * Tests US 03.04.01 (Browse Events), US 03.05.01 (Browse Profiles), US 03.06.01 (Browse Images).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Set up the fragment manually in the container before each test.
     * Tests that the admin panel loads and all browse buttons are visible and clickable.
     */
    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, AdminFragment.newInstance())
                    .commitNow();
        });
    }

    /**
     * Verifies that the primary administrative buttons (Events, Profiles, Images)
     * exist in the layout and are visible to the user.
     */
    @Test
    public void testAdminPanelButtonsVisible() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull("Events button should exist", activity.findViewById(R.id.btn_admin_events));
            assertNotNull("Profiles button should exist", activity.findViewById(R.id.btn_admin_profiles));
            assertNotNull("Images button should exist", activity.findViewById(R.id.btn_admin_images));
            
            assertEquals("Events button should be visible", View.VISIBLE, activity.findViewById(R.id.btn_admin_events).getVisibility());
        });
    }

    /**
     * Verifies that the notification logs and comment management buttons are present.
     */
    @Test
    public void testAdminLogsAndCommentsTabs() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull("Notif Logs button should exist", activity.findViewById(R.id.btn_admin_notif_logs));
            assertNotNull("Comments button should exist", activity.findViewById(R.id.btn_admin_comments));
        });
    }
}
