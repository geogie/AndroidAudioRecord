package com.example.george.androidaudiorecord.mySound.myMp3;

import android.media.AudioFormat;

/**
 * Created by George.ren on 2018/11/5.
 * Email:1063658094@qq.com
 * Describe:
 */
public enum PCMFormat {
    PCM_8BIT (1,AudioFormat.ENCODING_PCM_8BIT),
    PCM_16BIT (2, AudioFormat.ENCODING_PCM_16BIT);

    private int bytesPerFrame;
    private int audioFormat;

    PCMFormat(int bytesPerFrame, int audioFormat) {
        this.bytesPerFrame = bytesPerFrame;
        this.audioFormat = audioFormat;
    }
    public int getBytesPerFrame() {
        return bytesPerFrame;
    }
    public int getAudioFormat() {
        return audioFormat;
    }
}
