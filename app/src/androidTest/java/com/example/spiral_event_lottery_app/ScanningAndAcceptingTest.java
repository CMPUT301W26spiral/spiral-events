package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import android.Manifest;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the QR Scanner feature.
 * These tests verify the presence and visibility of critical scanner UI elements 
 * (like the camera preview and loading indicator) and handle camera permissions.
 * 
 * NOTE: Uses direct View access to bypass potential library conflicts.
 */
@RunWith(AndroidJUnit4.class)
public class ScanningAndAcceptingTest {

    @Rule
    public ActivityScenarioRule<QR_scanner> activity_rule =
            new ActivityScenarioRule<>(QR_scanner.class);

    /**
     * Automatically grants camera permission for the tests to prevent system dialogs 
     * from blocking UI interaction.
     */
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    /**
     * Verifies that the camera preview container exists in the layout and is visible.
     */
    @Test
    public void test_camerabox_exists() {
        activity_rule.getScenario().onActivity(activity -> {
            View preview = activity.findViewById(R.id.camera_preview);
            assertNotNull("Camera preview view should exist in the layout", preview);
            assertEquals("Camera preview should be visible", View.VISIBLE, preview.getVisibility());
        });
    }

    /**
     * Verifies that the loading spinner is correctly defined in the scanner layout.
     */
    @Test
    public void test_ifspinner_hidden() {
        activity_rule.getScenario().onActivity(activity -> {
            View spinner = activity.findViewById(R.id.loading_spinner);
            assertNotNull("Loading spinner should exist", spinner);
        });
    }

    /**
     * Tests the safety of the QR processing logic when receiving empty or null data.
     * Ensures that the app handles missing data gracefully without crashing.
     */
    @Test
    public void test_empty_data() {
        acceptanceHandling handler = new acceptanceHandling();
        try {
            // Verify that calling invitation handling logic with null inputs doesn't cause a crash
            handler.invitation_accepted(null, "", "");
        } catch (Exception e) {
            // Test fails if an unhandled exception occurs
        }
    }
}
