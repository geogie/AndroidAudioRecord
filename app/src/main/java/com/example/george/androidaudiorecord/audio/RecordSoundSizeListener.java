package com.example.george.androidaudiorecord.audio;

/**
 * Created by George.ren on 2018/8/12.
 * Describe:
 */
public interface RecordSoundSizeListener {
    /**
     * 实时返回音量大小
     *
     * @param soundSize 当前音量大小
     */
    void onSoundSize(int soundSize);
}
