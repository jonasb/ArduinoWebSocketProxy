package com.wigwamlabs.arduinowebsocketproxy;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Accessory {
    public interface Callback {
        void onReadFromAccessory(byte[] bytes);

        void onAccessoryError();
    }

    private static final String TAG = Accessory.class.getSimpleName();
    private final UsbAccessory mAccessory;
    private final Callback mCallback;
    private final ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private final Handler mHandler = new Handler();

    public Accessory(Context context, UsbAccessory accessory, Callback callback) {
        mAccessory = accessory;
        mCallback = callback;
        final UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mFileDescriptor = manager.openAccessory(accessory);
        final FileDescriptor fd = mFileDescriptor.getFileDescriptor();
        mInputStream = new FileInputStream(fd);
        mOutputStream = new FileOutputStream(fd);

        final Thread readerThread = new Thread("AccessoryReader") {
            @Override
            public void run() {
                final ByteBuffer buffer = ByteBuffer.allocate(16384);
                while (true) {
                    final FileInputStream stream = mInputStream;
                    if (stream == null) {
                        break;
                    }
                    try {
                        // read blocking
                        // https://groups.google.com/forum/?fromgroups=#!topic/android-developers/GJdCyieD8DY
                        final int read = stream.read(buffer.array());
                        if (read < 0) {
                            break;
                        }

                        // report read data
                        final byte[] buf = new byte[read];
                        buffer.position(0);
                        buffer.get(buf);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onReadFromAccessory(buf);
                            }
                        });

                    } catch (final IOException e) {
                        Log.e(TAG, "Error while reading from accessory", e);
                        break;
                    }
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onAccessoryError();
                    }
                });
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
                mCallback.onAccessoryError();
            }
        }
    }

    public UsbAccessory getAccessory() {
        return mAccessory;
    }
}
