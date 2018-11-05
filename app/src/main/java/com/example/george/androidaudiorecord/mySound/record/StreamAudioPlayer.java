package com.example.george.androidaudiorecord.mySound.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.AudioTrack.MODE_STREAM;

/**
 * Created by George.ren on 2018/11/5.
 * Email:1063658094@qq.com
 * Describe:音频播放
 * 多线程编程
 * AudioTrack 音频文件 --》流--》播放
 *
 */
public final class StreamAudioPlayer {
    private static final String TAG = "StreamAudioPlayer";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    public AtomicBoolean mIsPlaying;
    private StreamAudioPlayer.PLAY_STATUS mPlay_Status;
    private ExecutorService mPlayExecutorService;
    private String mReplayPath;
    private StreamAudioPlayer.AudioPlayCompeletedCallback mReplayCallBack;

    private StreamAudioPlayer() {
        this.mPlay_Status = StreamAudioPlayer.PLAY_STATUS.INIT;
        this.mIsPlaying = new AtomicBoolean(false);
        this.mPlayExecutorService = Executors.newSingleThreadExecutor();
    }

    public static StreamAudioPlayer getInstance() {
        return StreamAudioPlayer.StreamAudioPlayerHolder.INSTANCE;
    }

    public int play(String path, StreamAudioPlayer.AudioPlayCompeletedCallback playCompeletedCallback) {
        this.mPlay_Status = StreamAudioPlayer.PLAY_STATUS.START;
        if (path != null && !TextUtils.isEmpty(path)) {
            if (playCompeletedCallback == null) {
                Log.w("StreamAudioPlayer", "can't set empty play_compelete_callback");
                return 2;
            } else {
                File file = new File(path);
                if (file.exists()) {
                    if (this.mIsPlaying.compareAndSet(false, true)) {
                        this.mPlayExecutorService.execute(new StreamAudioPlayer.AudioTrackRunnable(path, playCompeletedCallback));
                        return 0;
                    } else {
                        return 1;
                    }
                } else {
                    return 4;
                }
            }
        } else {
            Log.w("StreamAudioPlayer", "can't set empty play_path");
            return 3;
        }
    }

    public int stopPlay() {
        this.mPlay_Status = StreamAudioPlayer.PLAY_STATUS.STOP;
        this.mIsPlaying.compareAndSet(true, false);
        return 0;
    }

    public void playWithInterrupt(String path, StreamAudioPlayer.AudioPlayCompeletedCallback callback) {
        if (this.mIsPlaying.get()) {
            this.mPlay_Status = StreamAudioPlayer.PLAY_STATUS.RESTART;
            this.mReplayPath = path;
            this.mReplayCallBack = callback;
            this.mIsPlaying.compareAndSet(true, false);
        } else {
            this.play(path, callback);
        }

    }

    private void onReplay() {
        if (!TextUtils.isEmpty(this.mReplayPath) && this.mReplayCallBack != null) {
            this.play(this.mReplayPath, this.mReplayCallBack);
        }

    }

    private class AudioTrackRunnable implements Runnable {
        private AudioTrack mAudioTrack;
        private int minBufferSize;
        private String mPlayPath;
        private byte[] buffer = null;
        private StreamAudioPlayer.AudioPlayCompeletedCallback mAudioPlayCompletedCallback;

        AudioTrackRunnable(String path, StreamAudioPlayer.AudioPlayCompeletedCallback playCompeletedCallback) {
            if (this.mAudioTrack != null) {
                this.mAudioTrack.release();
                this.mAudioTrack = null;
            }

            this.mAudioPlayCompletedCallback = playCompeletedCallback;
            this.mPlayPath = path;
            this.minBufferSize = AudioTrack.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            this.mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(this.minBufferSize, DEFAULT_BUFFER_SIZE), MODE_STREAM);
            this.buffer = new byte[Math.max(this.minBufferSize, DEFAULT_BUFFER_SIZE)];
        }
        public void run() {
            if (this.mAudioTrack != null && this.mAudioTrack.getState() == 1) {
                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(this.mPlayPath, "r");
                } catch (Exception e) {
                    Log.w("StreamAudioPlayer", "startplayed fail: " + e.getMessage());
                }
                try {
                    this.mAudioTrack.play();
                    Log.w("StreamAudioPlayer", "startplayed");
                } catch (Exception e) {
                    Log.w("StreamAudioPlayer", "startplayed fail: " + e.getMessage());
                }

                try {
                    while(StreamAudioPlayer.this.mIsPlaying.get()) {
                        int audioRecord = 0;
                        int size;
                        if (this.buffer != null) {
                            for(size = 1; size <= 15; ++size) {
                                byte buf = this.buffer[size];
                                if (buf == 0) {
                                    ++audioRecord;
                                }
                            }
                        }

                        if (file != null && this.buffer != null) {
                            size = file.read(this.buffer, 0, this.buffer.length);
                            if (size == -1) {
                                break;
                            }

                            if (audioRecord < 10) {
                                this.mAudioTrack.write(this.buffer, 0, size);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    this.mAudioTrack.flush();
                    this.mAudioTrack.stop();
                    this.mAudioTrack.release();
                    this.mAudioTrack = null;
                    Log.w("StreamAudioPlayer", "release sucess");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (file != null) {
                        file.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (StreamAudioPlayer.this.mPlay_Status.equals(StreamAudioPlayer.PLAY_STATUS.START)) {
                        this.mAudioPlayCompletedCallback.onAudioPlayCompeleted();
                        StreamAudioPlayer.this.stopPlay();
                    } else if (StreamAudioPlayer.this.mPlay_Status.equals(StreamAudioPlayer.PLAY_STATUS.RESTART)) {
                        StreamAudioPlayer.this.onReplay();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static final class StreamAudioPlayerHolder {
        private static final StreamAudioPlayer INSTANCE = new StreamAudioPlayer();

        private StreamAudioPlayerHolder() {
        }
    }

    public interface AudioPlayCompeletedCallback {
        @WorkerThread
        void onAudioPlayCompeleted();
    }

    public static enum PLAY_STATUS {
        INIT,
        START,
        STOP,
        RESTART;
        private PLAY_STATUS() {
        }
    }
}
