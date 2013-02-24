package com.wigwamlabs.arduinowebsocketproxy;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

public class NetworkUtils {
    public static String getIPAddress() {
        try {
            final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (final NetworkInterface intf : interfaces) {
                final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (final InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        final String sAddr = addr.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(sAddr)) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (final SocketException e) {
            // ignore
        }
        return "";
    }
}
