package com.example.george.androidaudiorecord.myaac;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.example.george.androidaudiorecord.mySound.myAAC.PCM2AACConfig;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by George.ren on 2018/11/7.
 * Email:1063658094@qq.com
 * Describe: 只负责录音--》回调：输出数据
 * 考虑线程安全录音
 * <p>
 * ExecutorService--》Executors.newSingleThreadExecutor()
 * 注意：如果多次重复执行execute，则会先完成当前的再执行之后的，依次执行。
 */
public class MyRecord {
    private static final String TAG = "MyRecord";
    static final int FLAG_RECORDER_STOP = 101;// 停止状态
    static final int FLAG_RECORDER_INIT = 0;// 初始状态
    private volatile int mCurrentFlag;// 标记,保留字段

    private ExecutorService mExecutorService;// 线程管理
    private final AtomicBoolean mIsRecording;// 是否在录制状态


    private MyRecord() {
        this.mCurrentFlag = FLAG_RECORDER_INIT;
        this.mIsRecording = new AtomicBoolean(false);
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    public static MyRecord getInstance() {
        return MyRecord.Instance.INSTANCE;
    }

    private static final class Instance {
        private static final MyRecord INSTANCE = new MyRecord();

        private Instance() {
        }
    }

    /**
     * @param audioDataCallback
     * @return
     */
    public synchronized boolean start(@NonNull MyRecord.OnAudioDataCallback audioDataCallback) {
        return this.start(PCM2AACConfig.AACConfig.DEFAULT_SAMPLE_RATE, PCM2AACConfig.AACConfig.channelConfig,
                PCM2AACConfig.AACConfig.audioFormat, audioDataCallback);
    }

    /**
     * 重复start有暂停效果
     *
     * @param sampleRate        采样率 16000
     * @param channelConfig     声道：AudioFormat.CHANNEL_IN_MONO
     * @param audioFormat       格式：AudioFormat.ENCODING_PCM_16BIT
     * @param audioDataCallback 回调数据
     * @return
     */
    public synchronized boolean start(int sampleRate, int channelConfig, int audioFormat, @NonNull MyRecord.OnAudioDataCallback audioDataCallback) {
        if (this.mIsRecording.get()) {
            this.stop();
            return false;
        } else {
            if (this.mIsRecording.compareAndSet(false, true)) {
                this.mExecutorService.execute(new MyRecord.AudioRecordRunnable(sampleRate, channelConfig, audioFormat, audioDataCallback));
                return true;
            } else {
                return false;
            }
        }

    }

    public synchronized void stop() {
        this.mCurrentFlag = FLAG_RECORDER_STOP;
        this.mIsRecording.compareAndSet(true, false);
    }

    public boolean isRecording() {
        return this.mIsRecording.get();
    }

    private class AudioRecordRunnable implements Runnable {
        private AudioRecord mAudioRecord;

        private int byteBufferSize;// if minBufferSize<byteBufferSize ==> user byteBufferSize
        private MyRecord.OnAudioDataCallback audioDataCallback;

        AudioRecordRunnable(int sampleRate, int channelConfig, int audioFormat, @NonNull MyRecord.OnAudioDataCallback audioDataCallback) {
            //===========out-init-star============
            this.audioDataCallback = audioDataCallback;
            //===========out-init-end=============
            byteBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            Log.d(TAG, "mAudioRecord:mic " + "sampleRate:" + sampleRate + " channelConfig:" + channelConfig + " audioFormat:" + audioFormat + " byteBufferSize:" + byteBufferSize);
            this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, this.byteBufferSize);
        }

        @Override
        public void run() {
            Log.d(TAG, "start-threadName:" + Thread.currentThread().getName());
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            if (this.mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    this.mAudioRecord.startRecording();
                    this.audioDataCallback.onBeginRecorder();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "startRecording fail: " + e.getMessage());
                    this.audioDataCallback.onError(AudioRecord.ERROR_INVALID_OPERATION, e.getMessage());
                    return;
                }
                while (MyRecord.this.mIsRecording.get()) {
                    Log.d(TAG, "recording-threadName:" + Thread.currentThread().getName() + " record:" + mAudioRecord.toString());
                    AudioDate audioDate = new AudioDate();
                    audioDate.buffer = ByteBuffer.allocateDirect(byteBufferSize);
                    audioDate.size = mAudioRecord.read(audioDate.buffer, byteBufferSize);
                    if (audioDate.size >= 0) {
                        audioDataCallback.onAudioData(audioDate);
                        continue;
                    } else if (audioDate.size == AudioRecord.ERROR_INVALID_OPERATION || audioDate.size == AudioRecord.ERROR_BAD_VALUE) {
                        continue;
                    } else if (audioDate.size == AudioRecord.ERROR_DEAD_OBJECT) {

                    } else {

                    }
                    onError(audioDate.size);
                }
                if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
            } else {
                Log.e(TAG, "startRecording fail: no STATE_INITIALIZED");
            }
            this.audioDataCallback.onRecordStop();
        }

        private void onError(int errorCode) {
            Log.w(TAG, "onError <0 : " + errorCode);
            MyRecord.this.stop();
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w(TAG, "record fail: ERROR_INVALID_OPERATION");
                this.audioDataCallback.onError(errorCode, "表示不恰当的方法导致的失败");
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "record fail: ERROR_BAD_VALUE");
                this.audioDataCallback.onError(errorCode, "表示不恰当的方法导致的失败");
            }

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
