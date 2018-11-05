package com.example.george.androidaudiorecord.myRecord.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by George.ren on 2018/11/2.
 * Email:1063658094@qq.com
 * Describe:
 * 多线程编程处理，AudioRecord--》data
 * 文件：pcm、wav（采用pcmFile to wavFile 并不是实时）
 */
public class MyAudioRecorder {
    public static final int AUDIO_TYPE_PCM = 0;
    public static final int AUDIO_TYPE_WAV = 1;
    public static final int AUDIO_TYPE_ELSE = -1;
    static final int DEFAULT_SAMPLE_RATE = 16000;
    static final int DEFAULT_BUFFER_SIZE = 2048;
    static final int FLAG_RECORDER_NORMAL = 0;
    static final int FLAG_RECORDER_NULL = 99;
    static final int FLAG_RECORDER_PLAYER = 100;
    static final int FLAG_RECORDER_STOP = 101;
    static final int FLAG_RECORDER_CANCEL = 102;
    static final int FLAG_RECORDER_EXCEPTION = 103;
    static final int FLAG_RECORDER_CANCEL_QUIET = 104;
    private static final String TAG = "XSAudioRecorder";
    private final AtomicBoolean mIsRecording;
    private volatile int mCurrentFlag;
    private ExecutorService mExecutorService;

    private MyAudioRecorder() {
        this.mCurrentFlag = 0;
        this.mIsRecording = new AtomicBoolean(false);
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    public static MyAudioRecorder getInstance() {
        return MyAudioRecorder.Instance.INSTANCE;
    }

    /**
     * @param savePath          文件路径：xx\xx\xx.wav
     * @param audioType         文件类型
     * @param audioDataCallback 回调
     * @return
     */
    public synchronized boolean start(String savePath, int audioType, @NonNull MyAudioRecorder.OnAudioDataCallback audioDataCallback) {
        return this.start(savePath, audioType, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, DEFAULT_BUFFER_SIZE, audioDataCallback);
    }

    public synchronized boolean start(String savePath, int audioType, int sampleRate, int channelConfig, int audioFormat, int bufferSize, @NonNull MyAudioRecorder.OnAudioDataCallback audioDataCallback) {
        if (this.mIsRecording.get()) {
            this.stop();
        }

        if (this.mIsRecording.compareAndSet(false, true)) {
            this.mExecutorService.execute(new MyAudioRecorder.AudioRecordRunnable(savePath, audioType, sampleRate, channelConfig, audioFormat, bufferSize, audioDataCallback));
            return true;
        } else {
            return false;
        }
    }

    public synchronized void stop() {
        this.mCurrentFlag = FLAG_RECORDER_STOP;
        this.mIsRecording.compareAndSet(true, false);
    }

    public boolean isRecording() {
        return this.mIsRecording.get();
    }

    public boolean cancel() {
        this.mCurrentFlag = FLAG_RECORDER_CANCEL;
        return this.mIsRecording.compareAndSet(true, false);
    }

    public boolean deleteSafe() {
        this.mCurrentFlag = FLAG_RECORDER_CANCEL_QUIET;
        return this.mIsRecording.compareAndSet(true, false);
    }

    private class AudioRecordRunnable implements Runnable {
        private final MyAudioRecorder.OnAudioDataCallback mAudioDataCallback;
        private final byte[] mByteBuffer;
        private final short[] mShortBuffer;
        private final int mByteBufferSize;
        private final int mShortBufferSize;
        private final int mAudioFormat;
        private AudioRecord mAudioRecord;
        private FileOutputStream mOutputStream;
        private int mAudioType;
        private PcmToWavUtil mPcmToWavUtil;
        private String mPcmPath;
        private String mWavPath;
        private int minBufferSize;
        private int sampleRate;
        private int channelConfig;
        private int audioFormat;
        private int byteBufferSize;
        private int bufferSizeInBytes;

        /**
         * @deprecated
         */
        @Deprecated
        AudioRecordRunnable(int sampleRate, int channelConfig, int audioFormat, int byteBufferSize, @NonNull MyAudioRecorder.OnAudioDataCallback audioDataCallback) {
            this.mAudioFormat = audioFormat;
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, this.mAudioFormat);
            this.mByteBufferSize = byteBufferSize;
            this.mShortBufferSize = this.mByteBufferSize / 2;
            this.mByteBuffer = new byte[this.mByteBufferSize];
            this.mShortBuffer = new short[this.mShortBufferSize];
            this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, byteBufferSize));
            this.mAudioDataCallback = audioDataCallback;
        }

        AudioRecordRunnable(String targetPath, int audioType, int sampleRate, int channelConfig, int audioFormat, int byteBufferSize, @NonNull MyAudioRecorder.OnAudioDataCallback audioDataCallback) {
            this.mAudioFormat = audioFormat;
            this.minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, this.mAudioFormat);
            this.sampleRate = sampleRate;
            this.channelConfig = channelConfig;
            this.audioFormat = audioFormat;
            this.byteBufferSize = byteBufferSize;
            this.mByteBufferSize = byteBufferSize;
            this.mShortBufferSize = this.mByteBufferSize / 2;
            this.mByteBuffer = new byte[this.mByteBufferSize];
            this.mShortBuffer = new short[this.mShortBufferSize];
            this.bufferSizeInBytes = Math.max(this.minBufferSize, byteBufferSize);
            this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, this.bufferSizeInBytes);
            this.mAudioDataCallback = audioDataCallback;

            try {
                String pcm = ".pcm";
                String wav = ".wav";
                String m = ".m";
                int end;
                if (targetPath.endsWith(pcm)) {
                    this.mPcmPath = targetPath;
                    end = targetPath.lastIndexOf(pcm);
                    targetPath = targetPath.substring(0, end);
                } else if (targetPath.endsWith(wav)) {
                    end = targetPath.lastIndexOf(wav);
                    this.mPcmPath = targetPath.substring(0, end) + pcm;
                } else if (targetPath.endsWith(m)) {
                    end = targetPath.lastIndexOf(m);
                    this.mPcmPath = targetPath.substring(0, end) + pcm;
                } else {
                    this.mPcmPath = targetPath + pcm;
                }

                File file = new File(this.mPcmPath);
                File parentFile;
                if (!file.exists()) {
                    parentFile = file.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                } else {
                    parentFile = new File(file.getAbsolutePath() + System.currentTimeMillis());
                    file.renameTo(parentFile);
                    parentFile.delete();
                }

                this.mOutputStream = new FileOutputStream(file);
                this.mAudioType = audioType;
                if (this.mAudioType == AUDIO_TYPE_WAV) {
                    this.mPcmToWavUtil = PcmToWavUtil.inStance(this.bufferSizeInBytes);
                    if (!targetPath.endsWith(wav) && !targetPath.endsWith(m)) {
                        this.mWavPath = targetPath + wav;
                    } else {
                        this.mWavPath = targetPath;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.e(TAG, "start:   start");
            if (this.mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    this.mAudioRecord.startRecording();
                    this.mAudioDataCallback.onBeginRecorder();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "startRecording fail: " + e.getMessage());
                    this.mAudioDataCallback.onError(AudioRecord.ERROR_INVALID_OPERATION, e.getMessage());
                    return;
                }
                int discardBytes = 3200;
                int ret;
                while (discardBytes > 0) {
                    ret = this.mByteBuffer.length < discardBytes ? this.mByteBuffer.length : discardBytes;
                    int readBytes = this.mAudioRecord.read(this.mByteBuffer, 0, ret);
                    if (readBytes < 0) {
                        break;
                    }
                    discardBytes -= readBytes;
                    Log.d(TAG, "discard: " + readBytes);
                }
                while (true) {
                    if (MyAudioRecorder.this.mIsRecording.get()) {
                        if (this.mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                            ret = this.mAudioRecord.read(this.mShortBuffer, 0, this.mShortBufferSize);
                            if (ret >= 0) {
                                byte[] data = this.short2byte(this.mShortBuffer, ret, this.mByteBuffer);
                                int size = ret * 2;
                                this.mAudioDataCallback.onAudioData(data, size);
                                try {
                                    this.mOutputStream.write(data, 0, size);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.w(TAG, "writing:   " + data + "  " + size + "  >=0 " + ret);
                                continue;
                            }
                            Log.w(TAG, "error:   ret :  " + ret);
                            if (ret == AudioRecord.ERROR_INVALID_OPERATION || ret == AudioRecord.ERROR_BAD_VALUE) {
                                continue;
                            }
                            if (ret == AudioRecord.ERROR_DEAD_OBJECT) {
                                this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, this.sampleRate, this.channelConfig, this.audioFormat, this.bufferSizeInBytes);
                            }
                            this.onError(ret);
                            break;
                        } else {
                            ret = this.mAudioRecord.read(this.mByteBuffer, 0, this.mByteBufferSize);
                            if (ret > 0) {
                                this.mAudioDataCallback.onAudioData(this.mByteBuffer, ret);
                                try {
                                    this.mOutputStream.write(this.mByteBuffer, 0, ret);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }
                            this.onError(ret);
                            break;
                        }

                    }
                    break;
                }
            }
            try {
                if (this.mOutputStream != null) {
                    this.mOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (this.mAudioRecord != null) {
                this.mAudioRecord.release();
                this.mAudioRecord = null;
            }
            if (this.mPcmToWavUtil != null) {
                Log.w(TAG, "convert:   " + this.mPcmPath + "  " + this.mWavPath);
                try {
                    AudioFormatConvertUtil.pcmToWav(new File(this.mPcmPath), new File(this.mWavPath), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.w(TAG, "pcmToWav:   stop");
            }
            switch (MyAudioRecorder.this.mCurrentFlag) {
                case FLAG_RECORDER_STOP:
                    this.mAudioDataCallback.onRecordStop();
                    break;
                case FLAG_RECORDER_CANCEL:
                    this.mAudioDataCallback.onCancel();
                case FLAG_RECORDER_EXCEPTION:
                default:
                    break;
                case FLAG_RECORDER_CANCEL_QUIET:
                    this.mAudioDataCallback.onCancelQuiet();
            }
            MyAudioRecorder.this.mCurrentFlag = FLAG_RECORDER_NORMAL;
            Log.e(TAG, "over stop :   start");
        }

        private byte[] short2byte(short[] sData, int size, byte[] bData) {
            if (size > sData.length || size * 2 > bData.length) {
                Log.w(TAG, "short2byte: too long short data array");
            }
            for (int i = 0; i < size; ++i) {
                bData[i * 2] = (byte) (sData[i] & 255);
                bData[i * 2 + 1] = (byte) (sData[i] >> 8);
            }
            return bData;
        }

        private void onError(int errorCode) {
            Log.w(TAG, "onError <0 : " + errorCode);
            MyAudioRecorder.this.stop();
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w(TAG, "record fail: ERROR_INVALID_OPERATION");
                this.mAudioDataCallback.onError(errorCode, "表示不恰当的方法导致的失败");
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "record fail: ERROR_BAD_VALUE");
                this.mAudioDataCallback.onError(errorCode, "表示不恰当的方法导致的失败");
            }

        }
    }

    private static final class Instance {
        private static final MyAudioRecorder INSTANCE = new MyAudioRecorder();

        private Instance() {
        }
    }

    public interface OnAudioDataCallback {
        void onBeginRecorder();

        void onRecordStop();

        void onCancel();

        void onCancelQuiet();

        @WorkerThread
        void onAudioData(byte[] data, int size);

        void onError(int code, String message);
    }
}
