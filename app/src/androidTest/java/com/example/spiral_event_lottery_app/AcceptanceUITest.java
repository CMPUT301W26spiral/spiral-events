package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for acceptanceHandling.
 * These tests run on a real device/emulator where Firebase is available.
 */
@RunWith(AndroidJUnit4.class)
public class AcceptanceUITest {

    @Test
    public void testInitializationOnAndroid() {
        // This will now pass because it's running on Android
        acceptanceHandling handler = new acceptanceHandling();
        assertNotNull(handler);
    }

    @Test
    public void testInputValidationOnAndroid() {
        acceptanceHandling handler = new acceptanceHandling();
        
        // Test that calling with nulls doesn't crash on a real device
        handler.invitation_accepted(InstrumentationRegistry.getInstrumentation().getTargetContext(), null, null);
        handler.invitation_declined(InstrumentationRegistry.getInstrumentation().getTargetContext(), "", "");
    }
}
