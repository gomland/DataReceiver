package my.home.ibgs.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final String TAG = "connected_thread";

    private boolean isRun = true;

    private BluetoothSocket mSocket = null;
    private BTStateHandler mCallback;

    private InputStream mInStream;
    private OutputStream mOutStream;

    public ConnectedThread(BluetoothSocket socket, BTStateHandler callback) {
        mSocket = socket;
        mCallback = callback;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
            mCallback.handleState(BTManager.BT_FAIL, null);
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;
    }

    public void run() {
        Log.d(TAG, "접속스레드시작");
        byte[] buffer = new byte[1024];
        int readBufferPosition = 0;

        while (isRun) {
            if(!Thread.currentThread().isInterrupted()){
                try {
                    int byteAvailable = mInStream.available();   // 수신 데이터 확인

                    if(byteAvailable > 0) {   // 데이터가 수신된 경우.
                        byte[] packetBytes = new byte[byteAvailable];
                        mInStream.read(packetBytes);
                        for(int i=0; i<byteAvailable; i++) {
                            byte ch = packetBytes[i];
                            if(ch == '\n') {  //개행을 만나면 이어 붙이기
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(buffer, 0, encodedBytes, 0, encodedBytes.length);

                                final String data = new String(encodedBytes, "US-ASCII"); //아스키 코드 형태로 변환
                                readBufferPosition = 0;
                                mCallback.handleState(BTManager.BT_RECEVIED, data);
                            }
                            else
                                buffer[readBufferPosition++] = ch;
                        }
                    }
                }
                catch (IOException e) {Log.d(TAG, "수신 에러 : " + e.getMessage()); }
            }
        }
        Log.d(TAG, "접속스레드종료");
    }

    public void write(byte[] buffer) {
        try {
            mOutStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    public void cancel() {
        isRun = false;

        try {
            if(mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }
}
