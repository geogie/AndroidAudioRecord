package com.example.george.androidaudiorecord.mySound.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.example.george.androidaudiorecord.mySound.constraint.AudioTypeEnum;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by George.ren on 2018/11/5.
 * Email:1063658094@qq.com
 * Describe:
 * 多线程编程
 * AudioRecord pcm-->wav以流的形式写入文件RandomAccessFile
 */
public final class StreamAudioRecorder {
    private static final String TAG = "StreamAudioRecorder";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    public static final int FLAG_RECORDER_NULL = 99;
    public static final int FLAG_RECORDER_PLAYER = 100;
    public static final int FLAG_RECORDER_STOP = 101;
    public static final int FLAG_RECORDER_CANCEL = 102;
    public static final int FLAG_RECORDER_EXCEPTION = 103;
    public AtomicBoolean mIsRecording;
    private ExecutorService mExecutorService;
    private Boolean fileHeader;
    private int mCurrentRecoredrFlag;
    private AudioTypeEnum mAudioType;
    private AtomicBoolean mIsExceptionFlag;
    private StreamAudioRecorder.AudioRecordRunnable mRunnable;
    private static StreamAudioRecorder sStreamAudioRecorder;

    private StreamAudioRecorder() {
        this.fileHeader = true;
        this.mCurrentRecoredrFlag = FLAG_RECORDER_NULL;
        this.mAudioType = AudioTypeEnum.WAV;
        Log.w("BaseSingEngine", "StreamAudioRecorder");
        this.mIsRecording = new AtomicBoolean(false);
        this.mIsExceptionFlag = new AtomicBoolean(false);
        this.mExecutorService = Executors.newSingleThreadExecutor();
    }

    public static StreamAudioRecorder getInstance() {
        return StreamAudioRecorder.StreamAudioRecorderHolder.INSTANCE;
    }

    public ExecutorService getmExecutorService() {
        return this.mExecutorService;
    }

    public int start(String path, @NonNull StreamAudioRecorder.AudioStartCompeletedCallback audioStartCompeletedCallback, @NonNull StreamAudioRecorder.AudioStopCompletedCallback stopCompletedCallback, @NonNull StreamAudioRecorder.AudioDataCallback audioDataCallback) {
        if (path != null && !TextUtils.isEmpty(path)) {
            if (this.mIsRecording.compareAndSet(false, true)) {
                this.mCurrentRecoredrFlag = FLAG_RECORDER_PLAYER;
                this.mRunnable = new StreamAudioRecorder.AudioRecordRunnable(AudioFormat.ENCODING_PCM_16BIT, audioStartCompeletedCallback, stopCompletedCallback, audioDataCallback, path);
                this.mExecutorService.execute(this.mRunnable);
                return 0;
            } else {
                return 2;
            }
        } else {
            Log.w("StreamAudioRecorder", "can't set empty  record_path");
            return 1;
        }
    }

    public int stop() {
        this.mIsRecording.compareAndSet(true, false);
        this.mCurrentRecoredrFlag = FLAG_RECORDER_STOP;
        this.mIsExceptionFlag.compareAndSet(false, true);
        return 0;
    }

    public int clean() {
        this.mIsRecording.compareAndSet(true, false);
        this.mCurrentRecoredrFlag = FLAG_RECORDER_CANCEL;
        this.mIsExceptionFlag.compareAndSet(false, true);
        return 0;
    }

    public int getmCurrentRecoredrFlag() {
        return this.mCurrentRecoredrFlag;
    }

    private RandomAccessFile fopen(String path) throws IOException {
        int CHANNELS = 1;
        int BITS = 16;
        int FREQUENCY = DEFAULT_SAMPLE_RATE;
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        } else {
            File file = f.getParentFile();
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        RandomAccessFile file1 = new RandomAccessFile(f, "rws");
        if (AudioTypeEnum.WAV == this.mAudioType && getInstance().fileHeader) {
            file1.writeBytes("RIFF");
            file1.writeInt(0);
            file1.writeBytes("WAVE");
            file1.writeBytes("fmt ");
            file1.writeInt(Integer.reverseBytes(16));
            file1.writeShort(Short.reverseBytes((short) 1));
            file1.writeShort(Short.reverseBytes((short) CHANNELS));
            file1.writeInt(Integer.reverseBytes(FREQUENCY));
            file1.writeInt(Integer.reverseBytes(CHANNELS * FREQUENCY * BITS / 8));
            file1.writeShort(Short.reverseBytes((short) (CHANNELS * BITS / 8)));
            file1.writeShort(Short.reverseBytes((short) (CHANNELS * BITS)));
            file1.writeBytes("data");
            file1.writeInt(0);
            Log.d("StreamAudioRecorder", "PCM to WAV");
        }

        Log.d("StreamAudioRecorder", "wav path: " + path);
        return file1;
    }

    private void fwrite(RandomAccessFile file, byte[] data, int offset, int size) throws IOException {
        file.write(data, offset, size);
    }

    private void fclose(RandomAccessFile file) throws IOException {
        try {
            if (AudioTypeEnum.WAV == this.mAudioType) {
                file.seek(4L);
                file.writeInt(Integer.reverseBytes((int) (file.length() - 8L)));
                file.seek(40L);
                file.writeInt(Integer.reverseBytes((int) (file.length() - 44L)));
                Log.d("StreamAudioRecorder", "wav size: " + file.length());
            }
        } finally {
            file.getFD().sync();
            file.close();
        }

    }

    public void setAudioType(AudioTypeEnum audioType) {
        this.mAudioType = audioType;
    }

    private class AudioRecordRunnable implements Runnable {
        private final StreamAudioRecorder.AudioDataCallback mAudioDataCallback;
        private final StreamAudioRecorder.AudioStartCompeletedCallback mAudioStartCompeletedCallback;
        private final StreamAudioRecorder.AudioStopCompletedCallback mAudioStopCompletedCallback;
        private final byte[] mByteBuffer;
        private final short[] mShortBuffer;
        private final int mByteBufferSize;
        private final int mShortBufferSize;
        private final int mAudioFormat;
        private RandomAccessFile file = null;
        private int minBufferSize;
        private AudioRecord mAudioRecord;

        AudioRecordRunnable(int audioFormat, @NonNull StreamAudioRecorder.AudioStartCompeletedCallback audioStartCompeletedCallback, @NonNull StreamAudioRecorder.AudioStopCompletedCallback stopCompletedCallback, @NonNull StreamAudioRecorder.AudioDataCallback audioDataCallback, String path) {
            if (this.mAudioRecord != null) {
                this.mAudioRecord.release();
                this.mAudioRecord = null;
            }
            this.minBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(this.minBufferSize, DEFAULT_BUFFER_SIZE));
            this.mAudioFormat = audioFormat;
            this.mByteBufferSize = Math.max(this.minBufferSize, DEFAULT_BUFFER_SIZE);
            this.mShortBufferSize = this.mByteBufferSize / 2;
            this.mByteBuffer = new byte[this.mByteBufferSize];
            this.mShortBuffer = new short[this.mShortBufferSize];
            this.mAudioDataCallback = audioDataCallback;
            this.mAudioStartCompeletedCallback = audioStartCompeletedCallback;
            this.mAudioStopCompletedCallback = stopCompletedCallback;
            try {
                this.file = StreamAudioRecorder.this.fopen(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            if (this.mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    this.mAudioRecord.startRecording();
                    Log.w("StreamAudioRecorder", "startRecorded");
                    this.mAudioStartCompeletedCallback.onAudioStartCompeleted();
                } catch (Exception e) {
                    Log.w("StreamAudioRecorder", "startRecording fail: " + e.getMessage());
                    this.mAudioDataCallback.onError(AudioRecord.ERROR_INVALID_OPERATION);
                }
                try {
                    int discardBytes = 3200;
                    int ret;
                    while (discardBytes > 0) {
                        ret = this.mByteBuffer.length < discardBytes ? this.mByteBuffer.length : discardBytes;
                        int readBytes = this.mAudioRecord.read(this.mByteBuffer, 0, ret);
                        if (readBytes <= 0) {
                            break;
                        }
                        discardBytes -= readBytes;
                        Log.d("StreamAudioRecorder", "discard: " + readBytes);
                    }
                    while (StreamAudioRecorder.this.mIsRecording.get()) {
                        if (this.mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                            ret = this.mAudioRecord.read(this.mByteBuffer, 0, this.mByteBufferSize);
                            StreamAudioRecorder.this.mIsExceptionFlag.compareAndSet(true, false);
                            if (ret <= 0) {
                                this.onError(ret);
                                Log.w("StreamAudioRecorder", "Recording error");
                                break;
                            }

                            Log.w("StreamAudioRecorder", "Recording .... " + StreamAudioRecorder.this.mCurrentRecoredrFlag);
                            this.mAudioDataCallback.onAudioData(this.mByteBuffer, ret, StreamAudioRecorder.this.mIsRecording, StreamAudioRecorder.this.mCurrentRecoredrFlag);
                            if (this.file != null) {
                                StreamAudioRecorder.this.fwrite(this.file, this.mByteBuffer, 0, ret);
                            }
                        }
                    }

                    if (StreamAudioRecorder.this.mIsExceptionFlag.get() && (StreamAudioRecorder.this.mCurrentRecoredrFlag == FLAG_RECORDER_STOP || StreamAudioRecorder.this.mCurrentRecoredrFlag == FLAG_RECORDER_CANCEL)) {
                        StreamAudioRecorder.this.mCurrentRecoredrFlag = FLAG_RECORDER_EXCEPTION;
                        Log.w("StreamAudioRecorder", "Recording .... " + StreamAudioRecorder.this.mCurrentRecoredrFlag);
                        this.mAudioDataCallback.onAudioData(this.mByteBuffer, 0, StreamAudioRecorder.this.mIsRecording, StreamAudioRecorder.this.mCurrentRecoredrFlag);
                    }
                } catch (Exception e) {
                    StreamAudioRecorder.this.mCurrentRecoredrFlag = FLAG_RECORDER_EXCEPTION;
                    Log.w("StreamAudioRecorder", "Recording[E] .... " + StreamAudioRecorder.this.mCurrentRecoredrFlag);
                    this.mAudioDataCallback.onAudioData(this.mByteBuffer, 0, StreamAudioRecorder.this.mIsRecording, StreamAudioRecorder.this.mCurrentRecoredrFlag);
                    e.printStackTrace();
                }
                try {
                    this.mAudioRecord.stop();
                    this.mAudioRecord.release();
                    this.mAudioRecord = null;
                    Log.w("StreamAudioRecorder", "release sucess");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (this.file != null) {
                        StreamAudioRecorder.this.fclose(this.file);
                        if (this.mAudioStopCompletedCallback != null) {
                            this.mAudioStopCompletedCallback.onAudioStopCompeleted();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        private byte[] short2byte(short[] sData, int size, byte[] bData) {
            if (size > sData.length || size * 2 > bData.length) {
                Log.w("StreamAudioRecorder", "short2byte: too long short data array");
            }

            for (int i = 0; i < size; ++i) {
                bData[i * 2] = (byte) (sData[i] & 255);
                bData[i * 2 + 1] = (byte) (sData[i] >> 8);
            }

            return bData;
        }

        private void onError(int errorCode) {
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w("StreamAudioRecorder", "record fail: ERROR_INVALID_OPERATION");
                this.mAudioDataCallback.onError(errorCode);
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Log.w("StreamAudioRecorder", "record fail: ERROR_BAD_VALUE");
                this.mAudioDataCallback.onError(errorCode);
            } else {
                Log.w("StreamAudioRecorder", "record fail: ERROR");
                this.mAudioDataCallback.onError(errorCode);
            }

        }
    }

    public interface AudioStopCompletedCallback {
        @WorkerThread
        void onAudioStopCompeleted();
    }

    public interface AudioStartCompeletedCallback {
        @WorkerThread
        void onAudioStartCompeleted();
    }

    public interface AudioDataCallback {
        @WorkerThread
        void onAudioData(byte[] data, int size, AtomicBoolean recording, int flag);

        void onError(int code);
    }

    private static final class StreamAudioRecorderHolder {
        private static final StreamAudioRecorder INSTANCE = new StreamAudioRecorder();

        private StreamAudioRecorderHolder() {
        }
    }
}

