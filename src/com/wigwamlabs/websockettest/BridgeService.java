package com.wigwamlabs.websockettest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BridgeService extends Service implements Accessory.Callback, WebSocketServer.Callback {
    public class LocalBinder extends Binder {
        public BridgeService getService() {
            return BridgeService.this;
        }
    }

    private static final String TAG = BridgeService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private Accessory mAccessory;
    private BroadcastReceiver mAccessoryDetachedBroadcastReceiver;
    private WebSocketServer mWebSocketServer;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        closeAccessory();
        closeWebSocketServer();
    }

    void openAccessory(final UsbAccessory accessory) {
        if (mAccessory != null) {
            mAccessory.close();
        }
        mAccessory = new Accessory(this, accessory, this);

        mAccessoryDetachedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                    final UsbAccessory closeAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (accessory.equals(closeAccessory)) {
                        closeAccessory();
                    }
                }
            }
        };
        final IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mAccessoryDetachedBroadcastReceiver, mIntentFilter);

        startWebSocketServer();
    }

    private void closeAccessory() {
        if (mAccessory != null) {
            mAccessory.close();
        }
        mAccessory = null;
        if (mAccessoryDetachedBroadcastReceiver != null) {
            unregisterReceiver(mAccessoryDetachedBroadcastReceiver);
            mAccessoryDetachedBroadcastReceiver = null;
        }

        closeWebSocketServer();
    }

    public void writeToAccessory(byte[] bytes) {
        Log.d(TAG, "writeToAccessory() " + new String(bytes));
        if (mAccessory != null) {
            mAccessory.write(bytes);
        }
    }

    @Override
    public void onReadFromAccessory(byte[] buf) {
        Log.d(TAG, "onReadFromAccessory() " + new String(buf));
        mWebSocketServer.send(buf);
    }

    private void startWebSocketServer() {
        if (mWebSocketServer != null) {
            return;
        }

        mWebSocketServer = new WebSocketServer(this);
    }

    private void closeWebSocketServer() {
        if (mWebSocketServer != null) {
            try {
                mWebSocketServer.stop(500);
            } catch (final Exception e) {
                Log.e(TAG, "Exception when closing server", e);
            }
            mWebSocketServer = null;
        }
    }

    @Override
    public void onWebSocketMessage(String message) {
        writeToAccessory(message.getBytes());
    }
}
