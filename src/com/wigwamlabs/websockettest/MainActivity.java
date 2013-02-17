package com.wigwamlabs.websockettest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;

import com.wigwamlabs.websockettest.BridgeService.LocalBinder;

public class MainActivity extends Activity implements ServiceConnection {
    protected BridgeService mService;
    private UsbAccessory mAccessory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindBridgeService();

        final Intent intent = getIntent();
        mAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
    }

    private void bindBridgeService() {
        bindService(new Intent(this, BridgeService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        final LocalBinder binder = (LocalBinder) service;
        mService = binder.getService();

        if (mAccessory != null) {
            mService.openAccessory(mAccessory);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mService = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("XXX", "onNewIntent()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
