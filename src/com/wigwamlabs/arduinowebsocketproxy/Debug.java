package com.wigwamlabs.arduinowebsocketproxy;

import android.util.Log;

public class Debug {
    private static final String TAG = "ArduinoWebSocketProxy";

    public static void logException(String msg, Exception ex) {
        Log.e(TAG, msg, ex);
    }

    public static void logLifecycle(String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, msg);
        }
    }
}
