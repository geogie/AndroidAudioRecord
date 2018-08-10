package com.example.george.androidaudiorecord.audio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.george.androidaudiorecord.R;

public class AudioRecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
    }
    public void onStart(View view) {
        RecordManager.getInstance().start();
    }

    public void onStop(View view) {
        RecordManager.getInstance().stop();
    }
}
