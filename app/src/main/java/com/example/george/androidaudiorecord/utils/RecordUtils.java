package com.example.george.androidaudiorecord.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by George.ren on 2018/8/12.
 * Describe:
 */
public class RecordUtils {
    /**
     * 获取录音的声音分贝值
     *
     * @return 声音分贝值
     */
    public static long getMaxDecibels(byte[] input) {
        float[] amplitudes = byteToFloat(input);
        if (amplitudes == null) {
            return 0;
        }
        float maxAmplitude = 2;
        for (float amplitude : amplitudes) {
            if (Math.abs(maxAmplitude) < Math.abs(amplitude)) {
                maxAmplitude = amplitude;
            }
        }
        return Math.round(20 * Math.log10(maxAmplitude)); //formula dB = 20 * log(a / a0);
    }


    public static float[] byteToFloat(byte[] input) {
        if (input == null) {
            return null;
        }
        int bytesPerSample = 2;
        ByteBuffer buffer = ByteBuffer.wrap(input);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = FloatBuffer.allocate(input.length / bytesPerSample);
        for (int i = 0; i < floatBuffer.capacity(); i++) {
            floatBuffer.put(buffer.getShort(i * bytesPerSample));
        }
        return floatBuffer.array();
    }

}
