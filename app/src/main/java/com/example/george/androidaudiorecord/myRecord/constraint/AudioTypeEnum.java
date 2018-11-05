package com.example.george.androidaudiorecord.myRecord.constraint;

/**
 * Created by George.ren on 2018/11/5.
 * Email:1063658094@qq.com
 * Describe:
 */
public enum AudioTypeEnum {
    PCM("pcm"),
    WAV("wav");

    private String value;

    private AudioTypeEnum(String value) {
        this.setValue(value);
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
