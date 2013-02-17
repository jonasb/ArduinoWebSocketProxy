package com.wigwamlabs.websockettest;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.java_websocket.handshake.ServerHandshake;

public class WebSocketClient extends org.java_websocket.client.WebSocketClient {
    private static final String TAG = WebSocketClient.class.getSimpleName();
    private static final URI TARGET;

    static {
        URI uri = null;
        try {
            uri = new URI("ws://127.0.0.1:" + 8080);
        } catch (final URISyntaxException e) {
            // ignore
        }
        TARGET = uri;
    }

    static void sendMessageAndWaitForAnswer(final String message) {
        final WebSocketClient client = new WebSocketClient();
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(3000);
                    client.send(message);
                    sleep(10000);
                    client.closeBlocking();
                } catch (final Exception e) {
                }
            }
        }.start();
    }

    WebSocketClient() {
        super(TARGET);
        connect();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "onOpen()");
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, String.format("onMessage(%s)", message));
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        Log.d(TAG, String.format("onMessage(%s)", new String(bytes.array())));
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "onError()", ex);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, String.format("onClose(%d, %s, %s)", code, reason, remote));
    }
}
