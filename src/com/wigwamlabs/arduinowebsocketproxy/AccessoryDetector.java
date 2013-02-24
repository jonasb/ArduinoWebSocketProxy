package com.wigwamlabs.arduinowebsocketproxy;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AccessoryDetector {
    protected static final String TAG = AccessoryDetector.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = AccessoryDetector.class.getCanonicalName().replace(AccessoryDetector.class.getSimpleName(), "USB_PERMISSION");
    private static final int TIME_BETWEEN_CHECKS_MS = 1000;
    private static final int MESSAGE_CHECK_ACCESSORY = 0;
    private final MainActivity mActivity;
    private final UsbManager mUsbManager;
    private final Handler mHandler;

    public AccessoryDetector(MainActivity activity) {
        mActivity = activity;
        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);

        activity.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MESSAGE_CHECK_ACCESSORY) {
                    detectAccessories();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_ACCESSORY, TIME_BETWEEN_CHECKS_MS);
                    return true;
                }
                return false;
            }
        });
    }

    public void onDestroy() {
        mActivity.unregisterReceiver(mBroadcastReceiver);
    }

    protected void detectAccessories() {
        final UsbAccessory a = getAvailableAccessory();
        mActivity.onAccessoryAvailable(a);
    }

    private UsbAccessory getAvailableAccessory() {
        final UsbAccessory[] as = mUsbManager.getAccessoryList();
        if (as != null && as.length > 0) {
            return as[0];
        }
        return null;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            if (!mHandler.hasMessages(MESSAGE_CHECK_ACCESSORY)) {
                mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_ACCESSORY, TIME_BETWEEN_CHECKS_MS);
            }
        } else {
            mHandler.removeMessages(MESSAGE_CHECK_ACCESSORY);
        }
    }

    public void connect() {
        final UsbAccessory accessory = getAvailableAccessory();
        if (accessory != null) {
            final PendingIntent pi = PendingIntent.getBroadcast(mActivity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(accessory, pi);
        }
    }

    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    final UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (accessory != null) {
                            mActivity.openAccessoryIfNeeded(accessory);
                        }
                    } else {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                }
            }
        }
    };
}
