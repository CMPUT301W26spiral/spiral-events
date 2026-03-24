package com.example.spiral_event_lottery_app;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import com.example.spiral_event_lottery_app.data.DeviceIdProvider;

/**
 * Unit tests for the DeviceIdProvider utility.
 */
public class DeviceIdProviderTest {

    @Test
    public void testClassExists() {
        // Simple test to ensure the provider class is functional
        DeviceIdProvider provider = new DeviceIdProvider();
        assertNotNull(provider);
    }
}
