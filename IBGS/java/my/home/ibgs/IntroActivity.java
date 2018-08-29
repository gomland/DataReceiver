package my.home.ibgs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by OWNER on 2017-02-01.
 */
public class IntroActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_intro);
        setTheme(android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);

        init();
    }

    private void init(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startDeviceAct();
            }
        }, 2000);
    }

    private void startDeviceAct(){
        Intent intent = new Intent(this.getApplicationContext(), ConnectAtivity.class);
        startActivity(intent);
        finish();
    }
}
