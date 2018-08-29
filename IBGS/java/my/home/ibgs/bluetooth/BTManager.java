package my.home.ibgs.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Shin on 2017-01-20.
 */
public class BTManager {
    private final String TAG = "BTManager";

    //private static final UUID UUID_TYPE = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");  //DEFAULT
    private static final UUID UUID_TYPE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Serial
    //private static final UUID UUID_TYPE = UUID.fromString("00001108-0000-1000-8000-00805F9B34FB"); //HeadsetServiceClass_UUID
    //private static final UUID UUID_TYPE = UUID.fromString("00000002-0000-1000-8000-00805F9B34FB"); //UDP

    public static final int BT_CONNECTED = 0;  //접속
    public static final int BT_FAIL = 1;     //접속 실패
    public static final int BT_RECEVIED = 2; //데이터 수신
    public static final int BT_LOST = 3;    //손실
    public static final int BT_SOCKET_ERR = 4;  //소켓 생성 실패

    public static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mSocket;

    private String mAddress; //선택된 기기 맥주소

    private AcceptThread mAcceptThread = null; //다른 기기로 부터 페어링을 받기 위한 스레드
    private ConnectThread mConnectThread = null;   //페어링 시도를 위한 스레드
    private ConnectedThread mConnectedThread = null; //페어링 후 데이터를 주고 받을 소켓을 열기 위한 스레드

    private BTStateHandler mBTStateHandler = null; //UI에서 상태를 업데이트 하기 위한 핸들러

    //매니저를 위한 싱글톤 패턴
    private static BTManager mBtManager = null;
    public static BTManager getInst(){
        if(mBtManager == null){
            mBtManager = new BTManager();
        }
        return mBtManager;
    }

    public BTManager(){
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //폰자체의 블루투스 기능 켜기 요청
    public boolean enableBT(Context context){
        if(!mBtAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)context).startActivityForResult(intent, REQUEST_ENABLE_BT);
            return false;
        }

        return true;
    }

    //주변 블루투스 기기 검색
    public void deviceScan(){
        cancelScan();
        mBtAdapter.startDiscovery();
    }

    //페어링된 디바이스를 가져옴
    public Set<BluetoothDevice> getPairedDevice(){
        return mBtAdapter.getBondedDevices();
    }

    public void cancelScan(){
        if (mBtAdapter.isDiscovering())
            mBtAdapter.cancelDiscovery();
    }

    //내 블루투스 검색 허용 요청
    public void discoverable(Context context){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        ((Activity)context).startActivity(intent);
    }

    //서버소켓을 열고 다른 블루투스의 접속을 기다린다.
    public synchronized void listen(){
        stop();

        mBtAdapter.cancelDiscovery();
        BluetoothServerSocket mServerSocket = null;
        try {
            mServerSocket = mBtAdapter.listenUsingRfcommWithServiceRecord(mBtAdapter.getName(), UUID_TYPE);
        } catch (IOException e) {
            sendHandler(BT_SOCKET_ERR, null);
            return;
        }
        mAcceptThread = new AcceptThread(mServerSocket, new BTStateHandler() {
            @Override
            public void handleState(int state, Object obj) {
                switch (state) {
                    case BT_CONNECTED: connected(); break;
                    case BT_FAIL: stop(); break;
                }

                sendHandler(state, obj);
            }
        });

        mAcceptThread.start();
    }

    //설정된 주소로 페어링 시도
    public synchronized void connect() {
        if(mAddress == null)
            return ;

        stop();

        Log.d(TAG, "요청 주소 : " + mAddress);
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(mAddress);
        mBtAdapter.cancelDiscovery();

        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID_TYPE);
        } catch (IOException e) {
            mSocket = null;
            sendHandler(BT_SOCKET_ERR, null);
            return;
        }

        if(mSocket != null) {
            mConnectThread = new ConnectThread(mSocket, new BTStateHandler() {
                @Override
                public void handleState(int state, Object obj) {
                    switch (state) {
                        case BT_CONNECTED: connected(); break;
                        case BT_FAIL: stop(); break;
                    }

                    sendHandler(state, obj);
                }
            });
            Log.d(TAG, "접속 시도");
            mConnectThread.start();
        }
    }

    //페어링 후 소켓 연결
    private synchronized void connected(){
        mConnectedThread = new ConnectedThread(mSocket, new BTStateHandler(){
            @Override
            public void handleState(int state, Object obj) {
                switch (state) {
                    case BT_FAIL: stop(); break;
                }

                sendHandler(state, obj);
            }
        });
        mConnectedThread.start();
    }

    //모든 스레드 종료
    public void stop(){
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mSocket = null;
    }

    private void sendHandler(int state, Object obj){
        if(mBTStateHandler != null)
            mBTStateHandler.handleState(state, obj);
    }

    //선택된 맥 주소를 저장
    public void setDeviceInfo(String address) {
        mAddress = address;
    }

    //화면에서의 상태를 업데이트 하기 위한 핸들러를 설정
    public void setStateHandler(BTStateHandler handler){
        mBTStateHandler = handler;
    }

    //업데이트 핸들러 제거
    public void clearStateHandler(){
        mBTStateHandler = null;
    }

    //연결된 기기로 메시지 전송
    public void sendMessage(String message){
        if(mConnectedThread != null){
            Log.d(TAG, "발신 : " + message);
            mConnectedThread.write(message.getBytes());
        }
    }
}
