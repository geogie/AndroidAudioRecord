package com.example.george.androidaudiorecord.audio;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.example.george.webtest.MyApp;

/**
 * Created by George.ren on 2018/8/10.
 * Describe:
 */
public class RecordService extends Service {
    private static final String TAG = RecordService.class.getSimpleName();

    private final static String ACTION_NAME = "action_type";

    private final static int ACTION_INVALID = 0;

    private final static int ACTION_START_RECORD = 1;

    private final static int ACTION_STOP_RECORD = 2;

    private final static int ACTION_RESUME_START = 3;

    private final static String PARAM_PATH = "path";

    public RecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_NAME)) {
            switch (bundle.getInt(ACTION_NAME, ACTION_INVALID)) {
                case ACTION_START_RECORD:
                    doStartRecording(bundle.getString(PARAM_PATH));
                    break;
                case ACTION_STOP_RECORD:
                    doStopRecording();
                    break;
                case ACTION_RESUME_START:
                    doResumeRecording();
                    break;
                default:
                    break;
            }
            return START_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public static void startRecording(String path) {
        Context context = MyApp.getInstance().getApplicationContext();
        Intent intent = new Intent(context, RecordService.class);
        intent.putExtra(ACTION_NAME, ACTION_START_RECORD);
        intent.putExtra(PARAM_PATH, path);
        context.startService(intent);
    }

    public static void stopRecording() {
        Context context = MyApp.getInstance().getApplicationContext();
        Intent intent = new Intent(context, RecordService.class);
        intent.putExtra(ACTION_NAME, ACTION_STOP_RECORD);
        context.startService(intent);
    }

    public static void resumeRecording() {
        Context context = MyApp.getInstance().getApplicationContext();
        Intent intent = new Intent(context, RecordService.class);
        intent.putExtra(ACTION_NAME, ACTION_RESUME_START);
        context.startService(intent);
    }


    private void doStartRecording(String path) {
        Logger.i(TAG, "doStartRecording path: %s", path);
        RecordHelper.getInstance().start(path);
    }

    private void doResumeRecording() {
        Logger.i(TAG, "doResumeRecording path: %s");
    }

    private void doStopRecording() {
        Logger.i(TAG, "doStopRecording");
        RecordHelper.getInstance().stop();
        stopSelf();
    }
}
