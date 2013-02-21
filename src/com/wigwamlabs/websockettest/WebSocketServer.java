package com.wigwamlabs.websockettest;

import android.os.Handler;
import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

final class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    interface Callback {
        void onWebSocketMessage(String message);

        void onWebSocketMessage(ByteBuffer message);

        void onWebSocketConnectionsChanged();
    }

    private static final String TAG = WebSocketServer.class.getSimpleName();
    protected WebSocket mWebSocketConnection;
    private final Callback mCallback;
    private final Handler mHandler = new Handler();

    WebSocketServer(Callback callback) {
        super(new InetSocketAddress(8080));
        mCallback = callback;
        // WebSocket.DEBUG = true;
        start();

        // TODO synchronize access to mWebSocketConnection
    }

    private void notifyConnectionsChanged() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onWebSocketConnectionsChanged();
            }
        });
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "onOpen()");
        if (mWebSocketConnection != null) {
            mWebSocketConnection.close(0); // TODO
        }
        mWebSocketConnection = conn;

        notifyConnectionsChanged();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "onClose()");
        if (mWebSocketConnection == conn) {
            mWebSocketConnection = null;

            notifyConnectionsChanged();
        }
    }

    public int getClientCount() {
        return (mWebSocketConnection == null ? 0 : 1);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.d(TAG, "onError()", ex);
    }

    @Override
    public void onMessage(WebSocket conn, final ByteBuffer message) {
        if (conn == mWebSocketConnection) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onWebSocketMessage(message);
                }
            });
        } else {
            conn.close(0); // TODO
        }
    }

    @Override
    public void onMessage(WebSocket conn, final String message) {
        Log.d(TAG, String.format("onMessage(%s)", message));
        if (conn == mWebSocketConnection) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onWebSocketMessage(message);
                }
            });
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
