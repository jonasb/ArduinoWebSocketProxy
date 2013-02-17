package com.wigwamlabs.websockettest;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Accessory {
    public interface Callback {
        void onReadFromAccessory(byte[] buf);
    }

    private static final String TAG = Accessory.class.getSimpleName();
    private final ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    public Accessory(Context context, UsbAccessory accessory, final Callback callback) {
        final UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mFileDescriptor = manager.openAccessory(accessory);
        final FileDescriptor fd = mFileDescriptor.getFileDescriptor();
        mInputStream = new FileInputStream(fd);
        mOutputStream = new FileOutputStream(fd);

        final Thread readerThread = new Thread("AccessoryReader") {
            @Override
            public void run() {
                while (mInputStream != null) {
                    final ByteBuffer buffer = ByteBuffer.allocate(16384);
                    try {
                        final int b = mInputStream.read();
                        if (b == -1) {
                            Log.d(TAG, "EOF");
                            break;
                        }
                        buffer.put((byte) b);
                        final int size = mInputStream.read(buffer.array(), buffer.position(), buffer.remaining()) + buffer.position();
                        final byte[] buf = new byte[size];
                        buffer.position(0);
                        buffer.get(buf);
                        callback.onReadFromAccessory(buf);

                    } catch (final IOException e) {
                        Log.d(TAG, "Error while reading from accessory", e);
                    }
                }

                Log.d(TAG, "Shutting down accessory reader thread");
            };
        };
        readerThread.start();

    }

    public void close() {
        if (mFileDescriptor != null) {
            try {
                mFileDescriptor.close();
            } catch (final IOException e) {
                Log.e(TAG, "Exception when closing file descriptor", e);
            }
        }

        mInputStream = null;
        mOutputStream = null;
    }

    public void write(byte[] bytes) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(bytes);
            } catch (final IOException e) {
                Log.e(TAG, "Exception when writing to accessory", e);
            }
        }
    }
}
