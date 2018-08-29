package my.home.ibgs.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class ConnectThread extends Thread {
    private final String TAG = "connect_thread";

    private BluetoothSocket mSocket = null;
    private BTStateHandler mCallback;

    public ConnectThread(final BluetoothSocket socket, BTStateHandler callback) {
        mSocket = socket;
        mCallback = callback;
    }

    public void run() {
        try {
            mSocket.connect();
            mCallback.handleState(BTManager.BT_CONNECTED, null);
        } catch (IOException e) {
            mCallback.handleState(BTManager.BT_FAIL, null);
            try {
                mSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, e2.getMessage());
            }
        }
    }

    public void cancel() {
        try {
            if(mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }
}
