package my.home.ibgs.data;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import my.home.ibgs.tools.Prefs;
import my.home.ibgs.tools.TransValue;
import my.home.ibgs.view.GraphView;

/**
 * Created by Shin on 2017-02-03.
 */
public class DataManager {
    private final String TAG = "DataManager";

    public static final String DEFAULT_PATH = "/Download/sheet.txt";
    private ArrayList<SheetData> mDataList;
    private int interval;

    public DataManager(){
        mDataList = new ArrayList<SheetData>();
    }

    public boolean init(String path){
        File data = new File(Environment.getExternalStorageDirectory() + "/" + path);

        Log.d(TAG, "파일경로 : " + data.getAbsolutePath());
        if(data.exists())
            return readFile(data);
        else
            Log.d(TAG, "파일이 존재하지 않습니다.");

        return false;
    }

    private boolean readFile(File file){
        if(file!=null && file.exists()){
            FileInputStream fis = null;
            BufferedReader bufferedReader = null;
            int cnt = 0;
            interval = 0;

            try {
                fis = new FileInputStream(file);
                bufferedReader = new BufferedReader(new InputStreamReader(fis, "utf-8"));
                String str;
                do{
                    str = bufferedReader.readLine();
                    if(str == null)
                        break;

                    if(cnt++ == 0) {
                        Log.d(TAG, "테이블 간격 : " + str);
                        interval = TransValue.toInteger(str);
                    }
                    else {
                        Log.d(TAG, "줄 읽기 : " + str);
                        pushData(str);
                    }
                }while(str != null);

                fis.close();
            } catch (Exception e) {
                Log.d(TAG, "파일 읽기 에러" + e.getMessage());
                return false;
            }
        }

        return true;
    }

    private void pushData(String data){
        try{
            StringTokenizer st = new StringTokenizer(data, "\t");
            SheetData sheetData = new SheetData();
            int idx = 0;

            Log.d(TAG, ">start");
            while(st.hasMoreTokens()){
                String str = st.nextToken();
                Log.d(TAG, ">" + str);
                if(idx > 2)
                    break;

                if(str!=null && str.length()>0)
                    sheetData.data[idx++] = TransValue.toInteger(str);
                else
                    sheetData.data[idx++] = 0;
            }
            Log.d(TAG, ">" + sheetData.data[0] + "," + sheetData.data[1] + "," + sheetData.data[2]);
            mDataList.add(sheetData);
        }catch(Exception e){
            Log.d(TAG, "데이터 파싱 에러");
        }
    }

    public int getParseData(int type, int value){
        if(mDataList.size() == 0 || type >= GraphView.SENSOR_MAX)
            return 0;

        int sheetValue = 0;
        int i;

        for(i=0; i<mDataList.size(); i++){
            sheetValue = mDataList.get(i).data[type];
            if(sheetValue > value)
                break;
        }

        sheetValue = i*interval;

        Log.d(TAG, type + "[" + value + ", " + sheetValue + "]");
        return sheetValue;
    }
}
