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
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.wigwamlabs.websockettest.BridgeService.LocalBinder;

public class MainActivity extends Activity implements ServiceConnection, BridgeService.Callback {
    protected BridgeService mService;
    private TextView mArduinoState;
    private TextView mWebSocketState;
    private TextView mWebSocketAddress;
    private TextView mLogReadFromAccessory;
    private TextView mLogWriteToAccessory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArduinoState = (TextView) findViewById(R.id.arduinoState);
        mWebSocketState = (TextView) findViewById(R.id.websocketState);
        mWebSocketAddress = (TextView) findViewById(R.id.websocketAddress);
        mLogReadFromAccessory = (TextView) findViewById(R.id.logReadFromAccessory);
        mLogWriteToAccessory = (TextView) findViewById(R.id.logWriteToAccessory);

        bindBridgeService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        openAccessoryIfNeeded(getAccessoryFromIntent());
    }

    private UsbAccessory getAccessoryFromIntent() {
        return (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
    }

    private void openAccessoryIfNeeded(UsbAccessory accessory) {
        if (mService == null) {
            return;
        }

        if (accessory != null) {
            mService.openAccessory(accessory);

            // WebSocketClient.sendMessageAndWaitForAnswer("Hello from client");
        }
    }

    private void bindBridgeService() {
        final Intent intent = new Intent(this, BridgeService.class);
        startService(intent);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        final LocalBinder binder = (LocalBinder) service;
        mService = binder.getService();

        mService.addCallback(this);

        openAccessoryIfNeeded(getAccessoryFromIntent());
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
    protected void onDestroy() {
        super.onDestroy();

        unbindService(this);
        mService = null;
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

        if (!connected) {
            if (mLogWriteToAccessory.getText().length() > 0) {
                mLogWriteToAccessory.append("\n--------------------");
            }
            if (mLogReadFromAccessory.getText().length() > 0) {
                mLogReadFromAccessory.append("\n--------------------");
            }
        }
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

    @Override
    public void onReadFromAccessory(byte[] bytes) {
        updateLog(mLogReadFromAccessory, bytes);
    }

    @Override
    public void onWriteToAccessory(byte[] bytes) {
        updateLog(mLogWriteToAccessory, bytes);
    }

    private void updateLog(TextView log, byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        final CharSequence existingText = log.getText();
        if (existingText.length() > 0 && existingText.charAt(existingText.length() - 1) == '-') {
            sb.append("\n");
        }
        for (final byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        log.append(sb.toString());
    }
}
