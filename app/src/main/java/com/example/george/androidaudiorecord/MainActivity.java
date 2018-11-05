package com.example.george.androidaudiorecord;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.george.androidaudiorecord.myRecord.record.MyAudioRecorder;
import com.example.george.androidaudiorecord.myRecord.record.StreamAudioRecorder;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void onBegin(View view) {
//        MyAudioRecorder.getInstance().start(getCachePath("xs") + "/xs_1" + ".wav", MyAudioRecorder.AUDIO_TYPE_WAV, new MyAudioRecorder.OnAudioDataCallback() {
//            @Override
//            public void onBeginRecorder() {
//                Log.d(TAG, "onBeginRecorder");
//            }
//
//            @Override
//            public void onRecordStop() {
//                Log.d(TAG, "onRecordStop");
//            }
//
//            @Override
//            public void onCancel() {
//                Log.d(TAG, "onCancel");
//            }
//
//            @Override
//            public void onCancelQuiet() {
//                Log.d(TAG, "onCancelQuiet");
//            }
//
//            @Override
//            public void onAudioData(byte[] var1, int var2) {
//                Log.d(TAG, "onAudioData");
//            }
//
//            @Override
//            public void onError(int var1, String var2) {
//                Log.d(TAG, "onError");
//            }
//        });

        StreamAudioRecorder.getInstance().start(getCachePath("xs") + "/xs_1" + ".wav", new StreamAudioRecorder.AudioStartCompeletedCallback() {
            @Override
            public void onAudioStartCompeleted() {
                Log.d(TAG,"onAudioStartCompeleted");
            }
        }, new StreamAudioRecorder.AudioStopCompletedCallback() {
            @Override
            public void onAudioStopCompeleted() {
                Log.d(TAG,"onAudioStopCompeleted");
            }
        }, new StreamAudioRecorder.AudioDataCallback() {
            @Override
            public void onAudioData(byte[] data, int size, AtomicBoolean recording, int flag) {
                Log.d(TAG,"onAudioData-data:"+data+" size:"+size+" recording:"+recording+" flag:"+flag);
            }

            @Override
            public void onError(int code) {
                Log.d(TAG,"onError-code:"+code);

            }
        });
    }

    public void onStop(View view) {
//        MyAudioRecorder.getInstance().stop();
        StreamAudioRecorder.getInstance().stop();
    }
}
