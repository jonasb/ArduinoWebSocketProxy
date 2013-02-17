package com.wigwamlabs.websockettest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize and start echo server
        final int port = 8080;
        WebSocketImpl.DEBUG = true;
        WebSocketServer server = new WebSocketServer(
                new InetSocketAddress(port)) {
            @Override
            public void onClose(WebSocket conn, int code, String reason,
                    boolean remote) {
                Log.d("SERVER", "onClose()");
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                Log.d("SERVER", "onError()", ex);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                Log.d("SERVER", String.format("onMessage(%s)", message));
                conn.send(message);
            }

            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                Log.d("SERVER", "onOpen()");
            }
        };

        server.start();

        // Initialize and start client
        URI uri = null;
        try {
            uri = new URI("ws://127.0.0.1:" + port);
        } catch (URISyntaxException e) {
            // ignore
        }

        WebSocketClient client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d("CLIENT", "onOpen()");
                send("Hello world");
            }

            @Override
            public void onMessage(String message) {
                Log.d("CLIENT", String.format("onMessage(%s)", message));
            }

            @Override
            public void onError(Exception ex) {
                Log.d("CLIENT", "onError()", ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d("CLIENT", String.format("onClose(%d, %s, %s)", code,
                        reason, remote));
            }
        };

        client.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
