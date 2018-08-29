package my.home.android.datareceiver;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DeviceButton extends android.support.v7.widget.AppCompatButton {
    private BluetoothDevice mBluetoothDevice;

    public DeviceButton(Context context, BluetoothDevice device) {
        super(context);

        mBluetoothDevice = device;
        setBackgroundColor(Color.DKGRAY);
        setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        setLayoutParams(params);
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }
}
