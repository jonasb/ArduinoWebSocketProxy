package com.wigwamlabs.websockettest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.wigwamlabs.websockettest.BridgeService.LocalBinder;

public class MainActivity extends Activity implements ServiceConnection, BridgeService.Callback {
    protected BridgeService mService;
    private UsbAccessory mAccessory;
    private TextView mArduinoState;
    private TextView mWebSocketState;
    private TextView mWebSocketAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArduinoState = (TextView) findViewById(R.id.arduinoState);
        mWebSocketState = (TextView) findViewById(R.id.websocketState);
        mWebSocketAddress = (TextView) findViewById(R.id.websocketAddress);

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

        mService.addCallback(this);

        if (mAccessory != null) {
            mService.openAccessory(mAccessory);

            // WebSocketClient.sendMessageAndWaitForAnswer("Hello from client");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mService = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mService != null) {
            mService.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mService != null) {
            mService.removeCallback(this);
        }
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

    @Override
    public void onAccessoryState(boolean connected) {
        final Resources res = getResources();
        mArduinoState.setText(connected ? R.string.accessory_connected : R.string.accessory_disconnected);
        mArduinoState.setTextColor(res.getColor(connected ? R.color.green : R.color.red));
    }

    @Override
    public void onWebSocketState(boolean running, int clients) {
        final Resources res = getResources();
        mWebSocketState.setText(running ? res.getQuantityString(R.plurals.websocket_running, clients, clients) : res.getString(R.string.websocket_stopped));
        mWebSocketState.setTextColor(res.getColor(running ? R.color.green : R.color.red));

        if (running) {
            mWebSocketAddress.setText(String.format("ws://%s:%d", NetworkUtils.getIPAddress(), 8080));
            mWebSocketAddress.setVisibility(View.VISIBLE);
        } else {
            mWebSocketAddress.setVisibility(View.GONE);
        }

    }
}
