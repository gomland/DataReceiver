package my.home.ibgs.data;

import my.home.ibgs.view.GraphView;

/**
 * Created by OWNER on 2017-02-03.
 */
public class SheetData {
    public int[] data;

    public SheetData(){
        data = new int[GraphView.SENSOR_MAX];
    }
}
