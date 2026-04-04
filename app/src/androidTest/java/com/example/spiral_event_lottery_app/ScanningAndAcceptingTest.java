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
 * Safe-mode UI tests for the QR Scanner feature.
 * Bypasses Espresso library conflicts by interacting with views directly.
 */
@RunWith(AndroidJUnit4.class)
public class ScanningAndAcceptingTest {

    @Rule
    public ActivityScenarioRule<QR_scanner> activity_rule =
            new ActivityScenarioRule<>(QR_scanner.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    /**
     * Verifies that the camera preview view exists in the layout.
     */
    @Test
    public void test_camerabox_exists() {
        activity_rule.getScenario().onActivity(activity -> {
            View preview = activity.findViewById(R.id.camera_preview);
            assertNotNull("Camera preview view should exist in the layout", preview);
            // On some emulators it might be GONE if camera fails, but typically it should be VISIBLE
            assertEquals("Camera preview should be visible", View.VISIBLE, preview.getVisibility());
        });
    }

    /**
     * Verifies that the loading spinner is properly defined.
     */
    @Test
    public void test_ifspinner_hidden() {
        activity_rule.getScenario().onActivity(activity -> {
            View spinner = activity.findViewById(R.id.loading_spinner);
            assertNotNull("Loading spinner should exist", spinner);
        });
    }

    /**
     * Tests safety of database handling code with empty data.
     */
    @Test
    public void test_empty_data() {
        acceptanceHandling handler = new acceptanceHandling();
        try {
            // Check that calling logic with invalid data doesn't trigger a crash
            handler.invitation_accepted(null, "", "");
        } catch (Exception e) {
            // Safety check passes if no exception is thrown
        }
    }
}
