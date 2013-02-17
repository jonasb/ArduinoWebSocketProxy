package com.wigwamlabs.websockettest;

import android.util.Log;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

final class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    interface Callback {
        void onWebSocketMessage(String message);
    }

    private static final String TAG = WebSocketServer.class.getSimpleName();
    protected WebSocket mWebSocketConnection;
    private final Callback mCallback;

    WebSocketServer(Callback callback) {
        super(new InetSocketAddress(8080));
        mCallback = callback;
        // WebSocket.DEBUG = true;
        start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "onOpen()");
        mWebSocketConnection = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "onClose()");
        if (mWebSocketConnection == conn) {
            mWebSocketConnection = null;
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.d(TAG, "onError()", ex);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, String.format("onMessage(%s)", message));
        if (conn == mWebSocketConnection) {
            mCallback.onWebSocketMessage(message);
        } else {
            conn.close(0); // TODO
        }
    }

    public void send(byte[] buf) {
        if (mWebSocketConnection != null) {
            mWebSocketConnection.send(buf);
        }
    }
}
