package com.example.george.androidaudiorecord.audio;

/**
 * Created by George.ren on 2018/8/12.
 * Describe:
 */
public interface RecordStateListener {
    /**
     * 当前的录音状态发生变化
     *
     * @param state 当前状态
     */
    void onStateChange(RecordHelper.RecordState state);

    /**
     * 录音错误
     *
     * @param error 错误
     */
    void onError(String error);
}
