package com.example.george.androidaudiorecord.myRecord.record;

import android.media.AudioRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by George.ren on 2018/11/2.
 * Email:1063658094@qq.com
 * Describe:
 */
public class PcmToWavUtil {
    private static PcmToWavUtil mInstance = null;
    private int mBufferSize;
    private int mSampleRate = 16000;
    private int mChannel = 16;
    private int mEncoding = 2;
    private ExecutorService mExecutorService;

    private PcmToWavUtil(int bufferSize) {
        this.mBufferSize = bufferSize;
        this.mExecutorService = Executors.newSingleThreadExecutor();
        this.mBufferSize = AudioRecord.getMinBufferSize(this.mSampleRate, this.mChannel, this.mEncoding);
    }

    private PcmToWavUtil(int sampleRate, int channel, int encoding) {
        this.mSampleRate = sampleRate;
        this.mChannel = channel;
        this.mEncoding = encoding;
        this.mBufferSize = AudioRecord.getMinBufferSize(this.mSampleRate, this.mChannel, this.mEncoding);
    }

    public static final PcmToWavUtil inStance(int bufferSize) {
        if (mInstance == null) {
            synchronized(PcmToWavUtil.class) {
                if (mInstance == null) {
                    mInstance = new PcmToWavUtil(bufferSize);
                }
            }
        }

        return mInstance;
    }

    public static final PcmToWavUtil newInStance(int bufferSize) {
        return new PcmToWavUtil(bufferSize);
    }

    public void pcmToWav(final String inFilename, final String outFilename) {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    AudioFormatConvertUtil.pcmToWav(new File(inFilename), new File(outFilename), true);
                } catch (IOException var2) {
                    var2.printStackTrace();
                }

            }
        };
        this.mExecutorService.submit(runnable);
    }

    /** @deprecated */
    @Deprecated
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[]{82, 73, 70, 70, (byte)((int)(totalDataLen & 255L)), (byte)((int)(totalDataLen >> 8 & 255L)), (byte)((int)(totalDataLen >> 16 & 255L)), (byte)((int)(totalDataLen >> 24 & 255L)), 87, 65, 86, 69, 102, 109, 116, 32, 16, 0, 0, 0, 1, 0, (byte)channels, 0, (byte)((int)(longSampleRate & 255L)), (byte)((int)(longSampleRate >> 8 & 255L)), (byte)((int)(longSampleRate >> 16 & 255L)), (byte)((int)(longSampleRate >> 24 & 255L)), (byte)((int)(byteRate & 255L)), (byte)((int)(byteRate >> 8 & 255L)), (byte)((int)(byteRate >> 16 & 255L)), (byte)((int)(byteRate >> 24 & 255L)), 4, 0, 16, 0, 100, 97, 116, 97, (byte)((int)(totalAudioLen & 255L)), (byte)((int)(totalAudioLen >> 8 & 255L)), (byte)((int)(totalAudioLen >> 16 & 255L)), (byte)((int)(totalAudioLen >> 24 & 255L))};
        out.write(header, 0, 44);
    }
}

