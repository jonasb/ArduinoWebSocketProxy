package com.wigwamlabs.arduinowebsocketproxy;

import android.os.Handler;

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

    protected WebSocket mWebSocketConnection;
    private final Callback mCallback;
    private final Handler mHandler = new Handler();

    WebSocketServer(Callback callback, int port) {
        super(new InetSocketAddress(port));
        mCallback = callback;
        // WebSocket.DEBUG = true;
        start();
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
    public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (mWebSocketConnection != null) {
            mWebSocketConnection.close(0); // TODO
        }
        mWebSocketConnection = conn;

        notifyConnectionsChanged();
    }

    @Override
    public synchronized void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (mWebSocketConnection == conn) {
            mWebSocketConnection = null;

            notifyConnectionsChanged();
        }
    }

    public synchronized int getClientCount() {
        return (mWebSocketConnection == null ? 0 : 1);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Debug.logException("WebSocket", ex);
    }

    @Override
    public synchronized void onMessage(WebSocket conn, final ByteBuffer message) {
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
    public synchronized void onMessage(WebSocket conn, final String message) {
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

    public synchronized void send(byte[] buf) {
        if (mWebSocketConnection != null) {
            mWebSocketConnection.send(buf);
        }
    }

    public synchronized String getConnectionInfo() {
        if (mWebSocketConnection == null) {
            return "";
        }
        return mWebSocketConnection.getRemoteSocketAddress().getAddress().getHostAddress();
    }
}
