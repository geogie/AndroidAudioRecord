package com.example.george.androidaudiorecord;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.george.androidaudiorecord.mySound.myAAC.PCM2AACConfig;
import com.example.george.androidaudiorecord.myaac.AudioDate;
import com.example.george.androidaudiorecord.myaac.MyAACFile;
import com.example.george.androidaudiorecord.myaac.MyRecord;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = "Main2Activity";
    private ArrayBlockingQueue queue;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


    }

    /**
     * 获取当前缓存目录
     *
     * @param cache
     * @return
     */
    public String getCachePath(String cache) {
        String headerDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            headerDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mytalspeech";
        } else {
            headerDir = getFilesDir().getAbsolutePath();
        }
        File file = new File(headerDir + cache);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onStart(View view) {
        PCM2AACConfig.setInit(PCM2AACConfig.TYPE_1);
        queue = new ArrayBlockingQueue<>(1024);
        String aacFile = getCachePath("xs") + "/xs_2-1" + ".aac";
        MyRecord.getInstance().start(new MyRecord.OnAudioDataCallback() {
            @Override
            public void onBeginRecorder() {
                Log.d(TAG, "onBeginRecorder");
            }

            @Override
            public void onRecordStop() {
                Log.d(TAG, "onRecordStop");
            }

            @Override
            public void onAudioData(AudioDate audioDate) {
                Log.d(TAG, "onAudioData:" + audioDate.toString());
                try {
                    Log.d(TAG, "queue:" + queue.size());
                    queue.put(audioDate);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(int code, String message) {
                Log.d(TAG, "onError-code:" + code + " message:" + message);
            }
        });

        MyAACFile.getInstance().start(aacFile, queue, new MyAACFile.OnAudioDataCallback() {
            @Override
            public void onBeginRecorder() {

            }

            @Override
            public void onRecordStop() {

            }

            @Override
            public void onAudioData(AudioDate audioDate) {

            }

            @Override
            public void onError(int code, String message) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onStop(View view) {
        MyRecord.getInstance().stop();
        MyAACFile.getInstance().stop();
    }
}
