package com.example.spiral_event_lottery_app.data;

import android.content.Context;
import android.provider.Settings;

/**
 * DeviceIdProvider is responsible for retrieving the unique Android Device ID
 * to identify the current user across all parts of the application.
 */
public class DeviceIdProvider {

    /**
     * Retrieves the unique Android Device ID.
     * This ID is consistent for the life of the app installation.
     */
    public static String getDeviceId(Context context) {
        // Return the real Android Secure ID instead of a random UUID
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
