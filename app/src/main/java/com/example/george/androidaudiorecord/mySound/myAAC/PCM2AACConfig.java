package com.example.george.androidaudiorecord.mySound.myAAC;

import android.media.AudioFormat;

/**
 * ADTS：采样率
 * 0: 96000 Hz
 * 1: 88200 Hz
 * 2: 64000 Hz
 * 3: 48000 Hz
 * 4: 44100 Hz
 * 5: 32000 Hz
 * 6: 24000 Hz
 * 7: 22050 Hz
 * 8: 16000 Hz
 * 9: 12000 Hz
 * 10: 11025 Hz
 * 11: 8000 Hz
 * 12: 7350 Hz
 * 13: Reserved
 * 14: Reserved
 * 15: frequency is written explictly
 * 给编码出的aac裸流添加adts头字段
 * <p>
 * 要空出前7个字节，否则会搞乱数据
 */
public class PCM2AACConfig {
    public static final int TYPE_1 = 1;
    public static final int TYPE_2 = 2;
    public static final int TYPE_3 = 3;

    public static void setInit(int type) {
        if (type == TYPE_1) {
            AACConfig.DEFAULT_SAMPLE_RATE = AACConfig1.DEFAULT_SAMPLE_RATE;
            AACConfig.audioFormat = AACConfig1.audioFormat;
            AACConfig.channelConfig = AACConfig1.channelConfig;
            AACConfig.audioFormat = AACConfig1.audioFormat;
            AACConfig.channelCount = AACConfig1.channelCount;
            AACConfig.freqIdx = AACConfig1.freqIdx;
            AACConfig.chanCfg = AACConfig1.chanCfg;
        } else if (type == TYPE_2) {
            AACConfig.DEFAULT_SAMPLE_RATE = AACConfig2.DEFAULT_SAMPLE_RATE;
            AACConfig.audioFormat = AACConfig2.audioFormat;
            AACConfig.channelConfig = AACConfig2.channelConfig;
            AACConfig.audioFormat = AACConfig2.audioFormat;
            AACConfig.channelCount = AACConfig2.channelCount;
            AACConfig.freqIdx = AACConfig2.freqIdx;
            AACConfig.chanCfg = AACConfig2.chanCfg;
        }
    }

    public static class AACConfig {
        public static int DEFAULT_SAMPLE_RATE = 44100;
        public static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        public static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        public static int channelCount = 1;
        public static int freqIdx = 4;// 44100
        public static int chanCfg = channelCount;// CPE
    }

    public interface AACConfig1 {
        /*google官方推荐：兼容最广的44100 单声道录制 */
        int DEFAULT_SAMPLE_RATE = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelCount = 1;
        int freqIdx = 4;// 44100
        int chanCfg = channelCount;// CPE
    }

    public interface AACConfig2 {
        /*目前厂商第三方语音识别常用的格式：16000 单声道 */
        int DEFAULT_SAMPLE_RATE = 16000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelCount = 1;
        int freqIdx = 8;// 16000
        int chanCfg = channelCount;// CPE
    }
}
