package com.example.spiral_event_lottery_app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * DeviceIdProvider is responsible for generating and retrieving a device identifier for the current user of the application
 * Note: This is a placeholder to test the app's functionality
 */

public class DeviceIdProvider {

    private static final String PREFS_NAME = "event_lottery_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    /**
     * Retrieves the unique device identifier used by the event lottery system.
     * If a device ID already exists, that value is returned. Otherwise, a new UUID is generated, saved locally, and then returned.
     */
    public static String getDeviceId(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String id = prefs.getString(KEY_DEVICE_ID, null);

        if (id == null) {
            id = UUID.randomUUID().toString();

            prefs.edit()
                    .putString(KEY_DEVICE_ID, id)
                    .apply();
        }

        return id;
    }
}