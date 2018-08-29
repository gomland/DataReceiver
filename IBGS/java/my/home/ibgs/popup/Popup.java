package my.home.ibgs.popup;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by OWNER on 2017-02-01.
 */
public class Popup {
    public static void alert(Context context, String text){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
