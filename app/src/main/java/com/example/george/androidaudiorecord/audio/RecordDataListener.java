package com.example.george.androidaudiorecord.audio;

/**
 * Created by George.ren on 2018/8/12.
 * Describe:
 */
public interface RecordDataListener {
    /**
     * 当前的录音状态发生变化
     *
     * @param data 当前音频数据
     */
    void onData(byte[] data);
}
