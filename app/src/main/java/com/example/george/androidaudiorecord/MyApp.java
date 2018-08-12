package com.example.george.androidaudiorecord;

import android.app.Application;

/**
 * Created by George.ren on 2018/8/10.
 * Describe:
 */
public class MyApp extends Application {
    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        this.instance = this;
        instance = this;
    }

    public static MyApp getInstance() {
        return instance;
    }
}
