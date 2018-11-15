package com.example.george.androidaudiorecord.myaac;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.example.george.androidaudiorecord.mySound.myAAC.PCM2AACConfig;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MyAACFile {
    private static final String TAG = "MyAACFile";
    static final int FLAG_RECORDER_INIT = 0;// 初始状态
    private volatile int mCurrentFlag;// 标记,保留字段
    private ExecutorService mExecutorService;// 线程管理
    private final AtomicBoolean mIsEncoding;// 是否在录制状态
    static final int FLAG_RECORDER_STOP = 101;// 停止状态


    private MyAACFile() {
        this.mCurrentFlag = FLAG_RECORDER_INIT;
        this.mIsEncoding = new AtomicBoolean(false);
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    public static MyAACFile getInstance() {
        return MyAACFile.Instance.INSTANCE;
    }

    private static final class Instance {
        private static final MyAACFile INSTANCE = new MyAACFile();

        private Instance() {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized boolean start(String aacFile, ArrayBlockingQueue queue, @NonNull OnAudioDataCallback audioDataCallback) {
        return this.start(aacFile, queue, PCM2AACConfig.AACConfig.DEFAULT_SAMPLE_RATE, PCM2AACConfig.AACConfig.channelConfig
                , PCM2AACConfig.AACConfig.audioFormat, PCM2AACConfig.AACConfig.channelCount,PCM2AACConfig.AACConfig.freqIdx, audioDataCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized boolean start(String aacFile, ArrayBlockingQueue queue, int sampleRate, int channelConfig, int audioFormat, int channelCount,int freqIdx, @NonNull OnAudioDataCallback audioDataCallback) {
        if (this.mIsEncoding.get()) {
            return false;
        } else {
            if (this.mIsEncoding.compareAndSet(false, true)) {
                this.mExecutorService.execute(new MyAACFile.EncodeRunnable(aacFile, queue, sampleRate, channelConfig, audioFormat, channelCount,freqIdx));
                return true;
            } else {
                return false;
            }
        }
    }

    public synchronized void stop() {
        this.mCurrentFlag = FLAG_RECORDER_STOP;
        this.mIsEncoding.compareAndSet(true, false);
    }

    private class EncodeRunnable implements Runnable {
        private MediaCodec mEncorder;
        private int minBufferSize;
        private OutputStream mFileStream;
        private String aacFile;
        private ArrayBlockingQueue queue;
        private int channelCount;
        private int freqIdx;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        EncodeRunnable(String aacFile, ArrayBlockingQueue queue, int sampleRateInHz, int channelConfig, int audioFormat, int channelCount,int freqIdx) {
            this.aacFile = aacFile;
            this.channelCount = channelCount;
            this.freqIdx = freqIdx;
            minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            try {
                mEncorder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelCount);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, sampleRateInHz * 16 * channelCount);// sampleRateInHz * bit * channelCount
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
            mEncorder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);// 需要mainWork
            this.queue = queue;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            if (mEncorder == null) {
                return;
            }
            mEncorder.start();
            try {
                mFileStream = new FileOutputStream(aacFile);
                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                AudioDate audioDate;
                Log.d(TAG, "start-mIsEncoding:" + mIsEncoding.get() + " queue:" + queue.size());
                while (MyAACFile.this.mIsEncoding.get() || queue.size() > 0) {
                    Log.d(TAG, "run-mIsEncoding:" + mIsEncoding.get() + " queue:" + queue.size());
                    // 从队列中取出录音的一帧音频数据
                    audioDate = getAudioDate();
                    if (audioDate == null) {
                        continue;
                    }
                    int inputBufferIndex = mEncorder.dequeueInputBuffer(-1);// -1:防止丢帧;0：可能丢帧；取出InputBuffer，填充音频数据，然后输送到编码器进行编码
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = mEncorder.getInputBuffer(inputBufferIndex);
                        inputBuffer.clear();
                        inputBuffer.put(audioDate.buffer);
                        mEncorder.queueInputBuffer(inputBufferIndex, 0, audioDate.size, System.nanoTime(), 0);
                    }
                    // 取出编码好的一帧音频数据，然后给这一帧添加ADTS头
                    int outputBufferIndex = mEncorder.dequeueOutputBuffer(mBufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        int outBitsSize = mBufferInfo.size;
                        ByteBuffer outputBuffer = mEncorder.getOutputBuffer(outputBufferIndex);
                        outputBuffer.position(mBufferInfo.offset);
                        outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                        int outPacketSize = outBitsSize + 7; // ADTS头部是7个字节
                        byte[] outData = new byte[outPacketSize];
                        addADTStoPacket(outData, outPacketSize);

                        outputBuffer.get(outData, 7, outBitsSize);
                        outputBuffer.position(mBufferInfo.offset);
                        mFileStream.write(outData);
                        mEncorder.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mEncorder.dequeueOutputBuffer(mBufferInfo, 0);
                    }
                }
                release();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 释放资源
         */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        public void release() {
            if (mFileStream != null) {
                try {
                    mFileStream.flush();
                    mFileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mEncorder != null) {
                mEncorder.stop();
            }
        }

        /**
         * 添加ADTS头
         *
         * @param packet
         * @param packetLen
         */
        private void addADTStoPacket(byte[] packet, int packetLen) {
            int profile = 2; //AAC LC
            int freqIdx = this.freqIdx; //44100 根据不同的采样率修改这个值
//            int chanCfg = 2; //CPE
            int chanCfg = channelCount; //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }

        /**
         * 从队列中取出一帧待编码的音频数据
         *
         * @return
         */
        public AudioDate getAudioDate() {
            if (queue != null) {
                try {
                    Log.d(TAG, "queue:" + queue.size());
                    return (AudioDate) queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public interface OnAudioDataCallback {
        @WorkerThread
        void onBeginRecorder();

        @MainThread
        void onRecordStop();

        @WorkerThread
        void onAudioData(AudioDate audioDate);

        @WorkerThread
        void onError(int code, String message);
    }
}
