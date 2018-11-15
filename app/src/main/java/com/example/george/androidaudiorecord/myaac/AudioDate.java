package com.example.george.androidaudiorecord.myaac;

import java.nio.ByteBuffer;

public class AudioDate {
    public ByteBuffer buffer;
    public int size;

    @Override
    public String toString() {
        return "AudioDate{" +
                "buffer=" + buffer +
                ", size=" + size +
                '}';
    }
}
