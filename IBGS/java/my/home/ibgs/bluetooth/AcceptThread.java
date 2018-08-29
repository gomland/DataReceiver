package my.home.ibgs.bluetooth;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class AcceptThread extends Thread {
    private final String TAG = "accept_thread@@";

    private boolean isAccept = false;

    private BluetoothServerSocket mServerSocket;
    private BTStateHandler mCallback;

    public AcceptThread(BluetoothServerSocket serverSocket, BTStateHandler callback) {
        mServerSocket = serverSocket;
        mCallback = callback;
    }

    public void run() {
        BluetoothSocket socket = null;
        while (!isAccept) {
            try {
                socket = mServerSocket.accept();
                mCallback.handleState(BTManager.BT_CONNECTED, socket);
                cancel();
            } catch (Exception e) {
                mCallback.handleState(BTManager.BT_FAIL, null);
            }
        }
    }

    public void cancel() {
        isAccept = true;
        try {
            if(mServerSocket != null)
                mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close mServerSocket", e);
        }
    }
}
