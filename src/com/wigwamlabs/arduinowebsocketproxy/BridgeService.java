package com.wigwamlabs.arduinowebsocketproxy;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BridgeService extends Service implements Accessory.Callback, WebSocketServer.Callback {
    interface Callback {
        void onAccessoryState(boolean connected);

        void onWebSocketState(boolean running, int port, int clients);

        void onWriteToAccessory(byte[] bytes);

        void onReadFromAccessory(byte[] bytes);
    }

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
    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private final Handler mHandler = new Handler();
    private int mClientCount;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mClientCount++;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mClientCount--;

        stopSelfIfPossible();

        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        mClientCount++;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        closeAccessory();
        closeWebSocketServer();
    }

    private void stopSelfIfPossible() {
        if (mAccessory == null && mClientCount == 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mAccessory == null && mClientCount == 0) {
                        stopSelf();
                    }
                }
            }, 10000);
        }
    }

    void addCallback(Callback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);

            notifyAccessoryStateChanged();
            notifyWebSocketStateChanged();
        }
    }

    void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    private void notifyWebSocketStateChanged() {
        final int clients = (mWebSocketServer == null ? 0 : mWebSocketServer.getClientCount());
        for (final Callback c : mCallbacks) {
            c.onWebSocketState(mWebSocketServer != null, mWebSocketServer != null ? mWebSocketServer.getPort() : 0, clients);
        }
    }

    private void notifyAccessoryStateChanged() {
        for (final Callback c : mCallbacks) {
            c.onAccessoryState(mAccessory != null);
        }
    }

    void openAccessory(final UsbAccessory accessory) {
        if (mAccessory != null) {
            return;
        }
        try {
            mAccessory = new Accessory(this, accessory, this);
        } catch (final Exception e) {
            Log.w(TAG, "Error when opening accessory: " + accessory, e);
            return;
        }
        notifyAccessoryStateChanged();

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
            mAccessory = null;
            notifyAccessoryStateChanged();
        }
        if (mAccessoryDetachedBroadcastReceiver != null) {
            unregisterReceiver(mAccessoryDetachedBroadcastReceiver);
            mAccessoryDetachedBroadcastReceiver = null;
        }

        closeWebSocketServer();

        stopSelfIfPossible();
    }

    public void writeToAccessory(byte[] bytes) {
        Log.d(TAG, "writeToAccessory() " + new String(bytes));
        if (mAccessory != null) {
            mAccessory.write(bytes);
            for (final Callback c : mCallbacks) {
                c.onWriteToAccessory(bytes);
            }
        }
    }

    @Override
    public void onReadFromAccessory(byte[] bytes) {
        Log.d(TAG, "onReadFromAccessory() " + new String(bytes));
        mWebSocketServer.send(bytes);
        for (final Callback c : mCallbacks) {
            c.onReadFromAccessory(bytes);
        }
    }

    @Override
    public void onAccessoryError() {
        Log.d(TAG, "onAccessoryError()");
        closeAccessory();
    }

    private void startWebSocketServer() {
        if (mWebSocketServer != null) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final int port = prefs.getInt("websocket_port", getResources().getInteger(R.integer.default_port));
        mWebSocketServer = new WebSocketServer(this, port);
        notifyWebSocketStateChanged();
    }

    private void closeWebSocketServer() {
        if (mWebSocketServer != null) {
            try {
                mWebSocketServer.stop(500);
            } catch (final Exception e) {
                Log.e(TAG, "Exception when closing server", e);
            }
            mWebSocketServer = null;
            notifyWebSocketStateChanged();
        }
    }

    @Override
    public void onWebSocketConnectionsChanged() {
        notifyWebSocketStateChanged();
    }

    @Override
    public void onWebSocketMessage(String message) {
        writeToAccessory(message.getBytes());
    }

    @Override
    public void onWebSocketMessage(ByteBuffer message) {
        writeToAccessory(message.array()); // TODO whole array?
    }
}
