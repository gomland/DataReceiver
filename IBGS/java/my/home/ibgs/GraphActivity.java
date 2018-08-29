package my.home.ibgs;

import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.net.TrafficStatsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import my.home.ibgs.bluetooth.BTManager;
import my.home.ibgs.bluetooth.BTStateHandler;
import my.home.ibgs.data.DataManager;
import my.home.ibgs.data.DataWriteManager;
import my.home.ibgs.popup.Popup;
import my.home.ibgs.tools.Constant;
import my.home.ibgs.tools.Prefs;
import my.home.ibgs.tools.TransValue;
import my.home.ibgs.view.GraphView;

public class GraphActivity extends AppCompatActivity {
    private final String TAG = "view_act";

    private DataManager mDataManager;

    private Animation mFadeOut;

    private TextView mTitle;
    private EditText mOffsetA, mOffsetB, mOffsetC;
    private Button mBackBtn, mScaleUpBtn, mScaleDownBtn, mRecordStart, mRecordStop, mOffsetBtn;
    private ImageView mReceiver, mDataWriter;
    private GraphView mGraphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_graph);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BTManager.getInst().clearStateHandler();
        BTManager.getInst().stop();
    }

    private void init(){
        initView();
        initData();
        initEvent();
        start();
    }

    //뷰정보 초기화
    private void initView(){
        mTitle = (TextView) findViewById(R.id.vi_title);
        mGraphView = (GraphView) findViewById(R.id.vi_draw_view);

        mReceiver = (ImageView) findViewById(R.id.vi_net_dot);
        mDataWriter = (ImageView) findViewById(R.id.vi_data_write);

        mOffsetA = (EditText) findViewById(R.id.vi_offset_1);
        mOffsetB = (EditText) findViewById(R.id.vi_offset_2);
        mOffsetC = (EditText) findViewById(R.id.vi_offset_3);
        mOffsetBtn = (Button)findViewById(R.id.vi_offset_btn);

        mBackBtn = (Button)findViewById(R.id.vi_back_btn);
        mScaleUpBtn = (Button)findViewById(R.id.vi_scale_up);
        mScaleDownBtn = (Button)findViewById(R.id.vi_scale_down);
        mRecordStart = (Button)findViewById(R.id.vi_save_start);
        mRecordStop = (Button)findViewById(R.id.vi_save_stop);
    }

    //변수 초기화
    private void initData(){
        //애니메이션 효과 로드
        mFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_out);

        //타이틀에 지정된 BT이름 표시
        String name = Prefs.get(getApplicationContext(), Constant.KEY_BT_SELECTED_NAME);
        mTitle.setText(name);

        String offsetA = Prefs.get(getApplicationContext(), Constant.KEY_OFFSET_A);
        String offsetB = Prefs.get(getApplicationContext(), Constant.KEY_OFFSET_B);
        String offsetC = Prefs.get(getApplicationContext(), Constant.KEY_OFFSET_C);
        updateOffset(offsetA, offsetB, offsetC);
        updateScale();

        //그래프 좌표 초기화
        setDrawData("A : 0 B : 0 C : 0");
    }

    //오프셋
    private void updateOffset(String offsetA, String offsetB, String offsetC){
        setOffset(GraphView.A, offsetA);
        setOffset(GraphView.B, offsetB);
        setOffset(GraphView.C, offsetC);
    }

    //오프셋 적용
    private void setOffset(int key, String value){
        if(value != null && value.length() > 0) {
            if(key == GraphView.A)
                mOffsetA.setText(value);
            else if(key == GraphView.B)
                mOffsetB.setText(value);
            else if(key == GraphView.C)
                mOffsetC.setText(value);
            mGraphView.setOffset(key, Integer.valueOf(value));
        }
        else{
            if(key == GraphView.A)
                mOffsetA.setText("0");
            else if(key == GraphView.B)
                mOffsetB.setText("0");
            else if(key == GraphView.C)
                mOffsetC.setText("0");
            mGraphView.setOffset(key, 0);
        }
    }

    //확대축소 값 계산
    private void updateScale(){
        String scale = Prefs.get(GraphActivity.this, Constant.KEY_SCALE);
        if(scale != null && scale.length()>0) {
            mGraphView.setMax((int)(Constant.DEFAULT_MAX * Float.valueOf(scale)));
        }
        else{
            Prefs.put(GraphActivity.this, Constant.KEY_SCALE, "1");
            mGraphView.setMax((int)Constant.DEFAULT_MAX);
        }
        upDateView(R.id.vi_scale_val, Constant.KEY_SCALE);
    }

    private void initEvent(){
        mOffsetBtn.setOnClickListener(btnClickListenr);
        mBackBtn.setOnClickListener(btnClickListenr);
        mScaleUpBtn.setOnClickListener(btnClickListenr);
        mScaleDownBtn.setOnClickListener(btnClickListenr);
        mRecordStart.setOnClickListener(btnClickListenr);
        mRecordStop.setOnClickListener(btnClickListenr);
    }

    //기능 시작
    private void start(){
        mDataManager = new DataManager();
        String filePath = Prefs.get(this, "file_path");
        if(filePath == null || (filePath != null && filePath.length() == 0))
            filePath = DataManager.DEFAULT_PATH;

        if(!mDataManager.init(filePath)){
            Popup.alert(this, "데이터 테이블을 불러오는데 실패하였습니다.");
            finish();
            return;
        }
        else
            Popup.alert(this, "데이터 테이블 읽기 성공.");

        //BTManager로부터 상태 값을 받기위한 콜백 등록
        BTManager.getInst().setStateHandler(new BTStateHandler() {
            @Override
            public void handleState(int state, Object obj) {
                if(obj != null){
                    Message msg = new Message();
                    msg.what = state;
                    msg.obj = obj;
                    mUiUpdateHandler.sendMessage(msg);
                }
            }
        });

        BTManager.getInst().connect(); //현재 설정 되있는 맥주소로 블루투스 접속
    }

    //줌 설정
    private void setZoom(float set){
        String scale = Prefs.get(getApplicationContext(), Constant.KEY_SCALE);
        double scaleVal = Float.valueOf(scale) + set;

        if(scaleVal < Constant.SCALE_SIZE)
            scaleVal = Constant.SCALE_SIZE;

        scaleVal = Math.round(scaleVal*10d) / 10d; //소수점 첫째자리 밑으로 제거

        Prefs.put(getApplicationContext(), Constant.KEY_SCALE, String.valueOf(scaleVal));
        mGraphView.setMax((int)(Constant.DEFAULT_MAX * scaleVal));
    }

    //설정된 확대 정보를 뷰에 표시
    private void upDateView(int id, String key){
        TextView offset = (TextView) findViewById(id);
        offset.setText(Prefs.get(getApplicationContext(), key));
    }

    //전송된 스트링을 잘라서 데이터화 시킴
    //A : 14524 B : 50393 C : 69696
    private final int DATA_SIZE = 5 + 4; //숫자 5, 구분자 4
    private void setDrawData(String data){
        String buf;
        String saveStr = "";

        while(data.length() > DATA_SIZE){
            buf = data.substring(0, DATA_SIZE+1);
            data = data.substring(DATA_SIZE+1, data.length());
            try {
                int target = -1;

                switch(buf.charAt(0)){  //첫 문자가 A 또는 B 또는 C 일때
                    case 'A': target = GraphView.A; saveStr += "A : "; break;
                    case 'B': target = GraphView.B; saveStr += "B : "; break;
                    case 'C': target = GraphView.C; saveStr += "C : "; break;
                }

                if(target != -1) { //switch에서 A 또는 B 또는 C가 감지되었다면
                    String subToken = buf.substring(4, buf.length()); //남은 스트링을 잘라 정수형으로 변환해서 날림, 콜론까지 제거를 위해 3부터 사용
                    if (subToken.length() > 0) {
                        int parseValue = mDataManager.getParseData(target, TransValue.toInteger(subToken));
                        saveStr += String.valueOf(parseValue + " ");
                        mGraphView.setData(target, parseValue);
                    }
                    else {
                        saveStr += String.valueOf("0 ");
                        mGraphView.setData(target, 0);
                    }
                    mReceiver.startAnimation(mFadeOut);
                }

            }catch(Exception e){
                Log.d(TAG, "파싱에러 : " + e.getMessage());
            }
        }

        if(mDataWriteManager != null && saveStr.length() > 0) {
            boolean success = mDataWriteManager.write(saveStr.substring(0, saveStr.length() - 1) + "\n");
            if(success)
                mDataWriter.startAnimation(mFadeOut);
        }

        mGraphView.update(); //그래프 다시그리기
    }

    //버튼 클릭에 대한 처리
    private View.OnClickListener btnClickListenr = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            switch(view.getId()){
                case R.id.vi_back_btn:
                    finish();
                    break;
                case R.id.vi_scale_down:
                    setZoom(Constant.SCALE_SIZE);
                    upDateView(R.id.vi_scale_val, Constant.KEY_SCALE);
                    break;
                case R.id.vi_scale_up:
                    setZoom(-Constant.SCALE_SIZE);
                    upDateView(R.id.vi_scale_val, Constant.KEY_SCALE);
                    break;
                case R.id.vi_save_start:
                    recordStart();
                    break;
                case R.id.vi_save_stop:
                    recordStop();
                    break;
                case R.id.vi_offset_btn:
                    try {
                        String offsetA = mOffsetA.getText().toString();
                        String offsetB = mOffsetB.getText().toString();
                        String offsetC = mOffsetC.getText().toString();
                        updateOffset(offsetA, offsetB, offsetC);
                        Prefs.put(getApplicationContext(), Constant.KEY_OFFSET_A, offsetA);
                        Prefs.put(getApplicationContext(), Constant.KEY_OFFSET_B, offsetB);
                        Prefs.put(getApplicationContext(), Constant.KEY_OFFSET_C, offsetC);
                    }catch(Exception e){
                        Log.d(TAG, e.getMessage());
                    }
                    break;
            }
        }
    };

    //상태 업데이트를 위한 핸들러
    private Handler mUiUpdateHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case BTManager.BT_CONNECTED:
                    Popup.alert(getApplicationContext(), "접속 완료");
                    break;
                case BTManager.BT_FAIL:
                    Popup.alert(getApplicationContext(), "접속 실패");
                    finish();
                    break;
                case BTManager.BT_LOST:
                    Popup.alert(getApplicationContext(), "접속이 끊어짐");
                    finish();
                    break;
                case BTManager.BT_RECEVIED:
                    try {
                        Log.d(TAG, "수신 : " + msg.obj.toString());
                        setDrawData(msg.obj.toString());
                    }catch(Exception e){
                        Log.d(TAG, "수신 오류 : " + e.getMessage());
                    }
                    break;
                case BTManager.BT_SOCKET_ERR:
                    Popup.alert(getApplicationContext(), "소켓생성 실패");
                    finish();
                    break;
            }
        }
    };

    //결과 저장 시작
    private DataWriteManager mDataWriteManager;
    private void recordStart(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName = sdfNow.format(date) + ".txt";

        mDataWriteManager = new DataWriteManager(fileName);
        if(mDataWriteManager.create()) {
            mRecordStart.setVisibility(View.GONE);
            mRecordStop.setVisibility(View.VISIBLE);
        }
        else
            mDataWriteManager = null;
    }

    //저장 종료
    private void recordStop(){
        if(mDataWriteManager != null) {
            mDataWriteManager.close();
            mDataWriteManager = null;
        }
        mRecordStart.setVisibility(View.VISIBLE);
        mRecordStop.setVisibility(View.GONE);
    }
}
