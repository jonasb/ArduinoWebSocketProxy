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

    public static void logWebSocket(String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, msg);
        }
    }

    public static void logWebSocket(String msg, byte[] bytes) {
        if (BuildConfig.DEBUG) {
            logWebSocket(msg + " " + bytesToString(bytes));
        }
    }

    private static String bytesToString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }
}
