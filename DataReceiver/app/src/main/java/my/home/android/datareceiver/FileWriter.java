package my.home.android.datareceiver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by Shin on 2017-02-03.
 */
public class FileWriter {
    private final String TAG = "FileWriter";

    private String mPath;
    private FileOutputStream mFos;
    private OutputStreamWriter mOw;
    private BufferedWriter mFileWriter;

    public FileWriter(String name){
        mPath = Environment.getExternalStorageDirectory() + "/Download/" + name;
    }

    public boolean create(){
        File file = new File(mPath);

        Log.d(TAG, "저장 파일 경로 : " + mPath);
        if(file.exists())
            return false;

        mFos = null;
        mOw = null;
        mFileWriter = null;
        try {
            mFos = new FileOutputStream(file);
            mOw = new OutputStreamWriter(mFos);
            mFileWriter = new BufferedWriter(mOw);
        } catch (Exception e) {
            Log.d(TAG, "파일 읽기 에러" + e.getMessage());
            mFos = null;
            mOw = null;
            mFileWriter = null;
            return false;
        }

        return true;
    }

    public boolean write(String data){
        if(mFileWriter == null)
            return false;

        try {
            mFileWriter.write(data);
            mFileWriter.flush();
        } catch (Exception e) {
            Log.d(TAG, "파일 쓰기 에러" + e.getMessage());
            return false;
        }

        return true;
    }

    public void close(){
        try {
            if(mFos != null)
                mFos.close();
            if(mOw != null)
                mOw.close();
            if(mFileWriter != null)
                mFileWriter.close();
        } catch (Exception e) {
            Log.d(TAG, "파일 닫기 에러" + e.getMessage());
        }
    }

}
