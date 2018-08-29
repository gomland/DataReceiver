package my.home.android.datareceiver;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;

import my.gomland.android.bluetooth.Bluetooth;
import my.gomland.android.bluetooth.BluetoothListener;


/**
 * Created by ShinSung on 2018-07-01.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothListener {
    private final int GRAPH_VIEW_SIZE = 5;
    private String[] SENSOR_NAME = {"A", "B", "C", "D1", "D2"};

    private ArrayList<GraphView> mGraphList = new ArrayList<>();
    private FileWriter mFileWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bluetooth.Helper.initialize(this, this);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 111);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bluetooth.Helper.disconnect();
    }

    @Override
    public void discoveryDevice(BluetoothDevice device) {
        addDevice(device);
    }

    @Override
    public void discoveryFinished() {
        findViewById(R.id.discovery_finish).setVisibility(View.GONE);
        findViewById(R.id.discovery_start).setVisibility(View.VISIBLE);
    }

    @Override
    public void connectionState(int state) {
        switch (state) {
            case Bluetooth.State.CONNECTED:
                findViewById(R.id.list_devices_scroll).setVisibility(View.GONE);
                findViewById(R.id.disconnect).setVisibility(View.VISIBLE);
                findViewById(R.id.discovery_start).setVisibility(View.GONE);
                findViewById(R.id.discovery_finish).setVisibility(View.GONE);
                findViewById(R.id.file_start).setVisibility(View.VISIBLE);
                createGraph(GRAPH_VIEW_SIZE);
                break;
            default:
                findViewById(R.id.disconnect).setVisibility(View.GONE);
                findViewById(R.id.discovery_start).setVisibility(View.VISIBLE);
                findViewById(R.id.file_start).setVisibility(View.GONE);
                findViewById(R.id.file_stop).setVisibility(View.GONE);
                onClickFileStop(null);
        }
    }

    @Override
    public void receiveMessage(String message) {
        updateData(message);
    }

    public void onClickFindDevice(View view) {
        ((LinearLayout) findViewById(R.id.list_devices)).removeAllViews();
        Bluetooth.Helper.startDiscovery();

        Set<BluetoothDevice> devices = Bluetooth.Helper.getPairedDevice();
        for (BluetoothDevice device : devices) {
            addDevice(device);
        }

        findViewById(R.id.list_devices_scroll).setVisibility(View.VISIBLE);
        findViewById(R.id.discovery_start).setVisibility(View.GONE);
        findViewById(R.id.discovery_finish).setVisibility(View.VISIBLE);
    }

    public void onClickDiscoveryFinished(View view) {
        Bluetooth.Helper.cancelDiscovery();
    }

    public void onClickDisconnect(View view) {
        Bluetooth.Helper.disconnect();
        findViewById(R.id.file_start).setVisibility(View.GONE);
        findViewById(R.id.file_stop).setVisibility(View.GONE);
    }

    public void onClickFileStart(View view) {
        if (mFileWriter == null) {
            String fileName = "log_" + getCurrentTime("yyyyMMdd_HHmmss") + ".txt";

            mFileWriter = new FileWriter(fileName);
            mFileWriter.create();
        }

        findViewById(R.id.file_start).setVisibility(View.GONE);
        findViewById(R.id.file_stop).setVisibility(View.VISIBLE);
    }

    public void onClickFileStop(View view) {
        if (mFileWriter != null) {
            mFileWriter.close();
            mFileWriter = null;
        }

        findViewById(R.id.file_start).setVisibility(View.VISIBLE);
        findViewById(R.id.file_stop).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof DeviceButton) {
            Bluetooth.Helper.connect(Bluetooth.UuidType.SERIAL, ((DeviceButton) view).getBluetoothDevice().getAddress());
            Bluetooth.Helper.cancelDiscovery();
        }
    }

    private void addDevice(BluetoothDevice device) {
        if(TextUtils.isEmpty(device.getName())){
            return;
        }

        DeviceButton button = new DeviceButton(this, device);
        button.setText(device.getName() + " (" + device.getAddress() + ")");
        button.setOnClickListener(this);
        ((LinearLayout) findViewById(R.id.list_devices)).addView(button);
    }

    private void createGraph(int size) {
        LinearLayout listGraph = findViewById(R.id.list_graph);
        listGraph.removeAllViews();
        mGraphList.clear();

        final int MARGIN = 10;
        int width = listGraph.getWidth() - MARGIN * 2;
        for (int i = 0; i < size; i++) {
            GraphView graphView = new GraphView(this, SENSOR_NAME[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, (int) (width * 0.25));
            params.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            graphView.setLayoutParams(params);
            listGraph.addView(graphView);
            mGraphList.add(graphView);
        }
    }

    private void updateData(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }

        StringTokenizer stringTokenizer = new StringTokenizer(message, " ");
        Double[] buffer = null;
        int idx = 0;

        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (TextUtils.isEmpty(token)) {
                continue;
            }

            if (token.equalsIgnoreCase("STN")) {
                buffer = new Double[GRAPH_VIEW_SIZE];
            } else if (token.contains("ETN")) {
                StringBuilder saveStringBuilder = new StringBuilder(getCurrentTime("yyyy-MM-dd HH:mm:ss "));

                for (int i = 0; i < mGraphList.size() && buffer != null; i++) {
                    if (buffer.length > i) {
                        mGraphList.get(i).setData(buffer[i]);
                        saveStringBuilder.append(buffer[i]).append(" ");
                    }
                }

                if (mFileWriter != null) {
                    if (!TextUtils.isEmpty(saveStringBuilder)) {
                        saveStringBuilder.append("\r\n");
                        mFileWriter.write(saveStringBuilder.toString());
                    }
                }
            } else if (buffer != null) {
                try {
                    buffer[idx] = Double.valueOf(token);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                idx++;
            }
        }
    }

    private String getCurrentTime(String format) {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat(format);
        return sdfNow.format(date);
    }
}
