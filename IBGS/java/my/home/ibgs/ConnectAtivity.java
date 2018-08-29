package my.home.ibgs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Set;

import my.home.ibgs.bluetooth.BTManager;
import my.home.ibgs.data.DataManager;
import my.home.ibgs.popup.Popup;
import my.home.ibgs.tools.Constant;
import my.home.ibgs.tools.PermissionChecker;
import my.home.ibgs.tools.Prefs;

public class ConnectAtivity extends AppCompatActivity {
    private final String TAG = "ConnectAtivity";
    private LinearLayout mBToffLayout, mBTonLayout, mScanDeviceList;
    private TextView mSelectedDeviceName, mFilePath;
    private Button mBtOnBtn, mScanBtn, mConnectBtn, mFileChooseBtn;
    private CheckBox mAutoCheck;

    private final int BT_OFF = 0;
    private final int BT_ON = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_connect);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBtStateReceiver);
    }

    private void init(){
        initView();
        initData();
        initEvent();
        start();
    }

    private void initView(){
        mBToffLayout = (LinearLayout) findViewById(R.id.dc_bt_off_layout);
        mBTonLayout = (LinearLayout) findViewById(R.id.dc_bt_on_layout);
        mScanDeviceList = (LinearLayout) findViewById(R.id.dc_device_scan_list);
        mSelectedDeviceName = (TextView) findViewById(R.id.dc_selected_name);
        mFilePath = (TextView) findViewById(R.id.dc_file_text);
        mBtOnBtn = (Button) findViewById(R.id.dc_bt_seton_btn);
        mScanBtn = (Button) findViewById(R.id.dc_scan_btn);
        mConnectBtn = (Button) findViewById(R.id.dc_connect_btn);
        mFileChooseBtn = (Button) findViewById(R.id.dc_choose_file_btn);
        mAutoCheck = (CheckBox) findViewById(R.id.dc_auto_check);
    }

    private void initData(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBtStateReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBtStateReceiver, filter);

        String filePath = Prefs.get(this, "file_path");
        if(filePath == null || (filePath != null && filePath.length() == 0))
            filePath = DataManager.DEFAULT_PATH;
        mFilePath.setText(filePath);

        PermissionChecker.verifyStoragePermissions(this);
    }

    private void initEvent(){
        //기기 스캔 버튼
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mScanBtn.setClickable(false); //버튼 상태 변경
                mScanBtn.setAlpha(0.35f); //투명도 35%

                mScanDeviceList.removeAllViews();
                setPairedDevice();
                BTManager.getInst().deviceScan();
            }
        });

        //접속 버튼
        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectAction();
            }
        });

        //블루투스 켜기 버튼
        mBtOnBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                BTManager.getInst().enableBT(ConnectAtivity.this);
            }
        });

        //자동로그인 체크 박스
        mAutoCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean check) {
                if(check)
                    Prefs.put(ConnectAtivity.this, Constant.KEY_AUTO_LOGIN, "Y");
                else
                    Prefs.put(ConnectAtivity.this, Constant.KEY_AUTO_LOGIN, "");
            }
        });

        //파일선택
        mFileChooseBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });
    }

    //해당 액티비티 기능 시작
    private void start(){
        boolean isBTEnabled = BTManager.getInst().enableBT(this);  //블루투스가 켜져있는지 체크하고 UI를 변경해줌
        setLayoutState(isBTEnabled ? BT_ON : BT_OFF);

        //저장된 맥주소가 있는지 확인 하고 있다면 표시해줌
        String macAddress = Prefs.get(getApplicationContext(), Constant.KEY_BT_SELECTED_MAC);
        if(macAddress != null && macAddress.length() > 0){
            String name = Prefs.get(getApplicationContext(), Constant.KEY_BT_SELECTED_NAME);
            mSelectedDeviceName.setText(name);
            BTManager.getInst().setDeviceInfo(macAddress);
        }

        //자동 로그인 설정이 되어있다면
        String autoLogin = Prefs.get(ConnectAtivity.this, Constant.KEY_AUTO_LOGIN);
        if(autoLogin != null && autoLogin.equals("Y")){
            mAutoCheck.setChecked(true);
            if(isBTEnabled)
                connectAction();
        }
        else
            mAutoCheck.setChecked(false);
    }

    private void setPairedDevice(){
        Set<BluetoothDevice> pairedDevices = BTManager.getInst().getPairedDevice();

        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices)
                addScanItem(device.getName(), device.getAddress(), true);
        }
    }

    //선택된 기기로 접속을 시도
    private void connectAction(){
        String macAddress = Prefs.get(getApplicationContext(), Constant.KEY_BT_SELECTED_MAC);

        if(macAddress == null ||
                (macAddress != null && macAddress.length() == 0)){
            Popup.alert(getApplicationContext(), "선택된 디바이스가 없습니다.");
        }
        else{
            BTManager.getInst().cancelScan();
            BTManager.getInst().setDeviceInfo(macAddress);

            Intent intent = new Intent(getApplicationContext(), GraphActivity.class);
            startActivity(intent);
        }
    }

    //블루투스 꺼진 상태와 켜진 상태에 따른 레이아웃 변화
    private void setLayoutState(int state){
        if(state == BT_ON){
            mBToffLayout.setVisibility(View.GONE);
            mBTonLayout.setVisibility(View.VISIBLE);
        }
        else{
            mBToffLayout.setVisibility(View.VISIBLE);
            mBTonLayout.setVisibility(View.GONE);
        }
    }

    //검색된 기기에 대한 뷰를 그리는 곳
    private void addScanItem(final String name, final String macAddress, boolean isPaired){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout deviceItem = (LinearLayout)inflater.inflate(R.layout.item_scan_device, mScanDeviceList, false);

        ImageView icon = (ImageView) deviceItem.findViewById(R.id.item_icon);
        //페어링 되어있는 기기는 파란불, 아니면 회색
        icon.setImageResource(isPaired ? R.drawable.icon_scan_device_pair : R.drawable.icon_scan_device);

        TextView nameTextView = (TextView) deviceItem.findViewById(R.id.item_name);
        if(name != null && name.length() > 0)
            nameTextView.setText(name);
        else
            nameTextView.setText("이름없음");

        TextView macTextView = (TextView) deviceItem.findViewById(R.id.item_mac);
        macTextView.setText(macAddress);

        //스캔 된 아이템을 선택했을때
        Button selectedBtn = (Button) deviceItem.findViewById(R.id.item_select_btn);
        selectedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedDeviceName.setText(name);
                Prefs.put(getApplicationContext(), Constant.KEY_BT_SELECTED_NAME, name);
                Prefs.put(getApplicationContext(), Constant.KEY_BT_SELECTED_MAC, macAddress);
            }
        });

        mScanDeviceList.addView(deviceItem);
    }

    //안드로이드에 BT상태 셋팅에 대한 결과를 받는 곳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("bt", "request Code : " + requestCode + ", resultCode : " + resultCode);
        switch(requestCode){
            case BTManager.REQUEST_ENABLE_BT: //블루투스 기능 켰을때
                if(resultCode == 0) {
                    Popup.alert(this.getApplicationContext(), "블루투스가 활성화를 취소하였습니다.");
                    setLayoutState(BT_OFF);
                }
                else if(resultCode == -1) {
                    Popup.alert(this.getApplicationContext(), "블루투스가 활성화 되었습니다.");
                    setLayoutState(BT_ON);
                }
                break;
            case FILE_SELECT_CODE:
                try {
                    Uri uri = data.getData();
                    if (uri != null) {
                        String filePath = uri.getPath();
                        String[] st = filePath.split(":");
                        if(st.length > 1) {
                            Prefs.put(getApplicationContext(), "file_path", st[1]);
                            mFilePath.setText(st[1]);
                            Log.d(TAG, " 경로저장 완료 : " + st[1]);
                        }
                    }
                }catch(Exception e){
                    Log.d(TAG, "FILE_SELECT_CODE result :  " + e.getMessage());
                }
                break;
        }
    }

    //블루투스 검색에 대한 결과를 받는 곳
    private BroadcastReceiver mBtStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) { //검색된 기기가 있을때
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                    addScanItem(device.getName(), device.getAddress(), false);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { //스캔 모드가 시간이 지나서 꺼졌을때
                mScanBtn.setClickable(true);
                mScanBtn.setAlpha(1);
            }
        }
    };

    //파일 선택 다이얼로그
    private static final int FILE_SELECT_CODE = 99;
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "데이터 시트 선택"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }
}
