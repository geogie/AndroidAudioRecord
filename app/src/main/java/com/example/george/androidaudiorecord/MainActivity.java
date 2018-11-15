package com.example.george.androidaudiorecord;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int sampleRateInHz = 44100;
    private final int channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private class AudioDate {
        private ByteBuffer buffer;
        private int size;
    }

    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private AudioRecorder mAudioRecorder;
    private AudioEncorder mAudioEncorder;

    private ArrayBlockingQueue queue;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = new ArrayBlockingQueue<>(1024);
        mAudioRecorder = new AudioRecorder();
        mAudioEncorder = new AudioEncorder();
    }

    /**
     * 获取当前缓存目录
     *
     * @param cache
     * @return
     */
    public String getCachePath(String cache) {
        String headerDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            headerDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mytalspeech";
        } else {
            headerDir = getFilesDir().getAbsolutePath();
        }
        File file = new File(headerDir + cache);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    public void onBegin(View view) {
//        MyAudioRecorder.getInstance().start(getCachePath("xs") + "/xs_1" + ".wav", MyAudioRecorder.AUDIO_TYPE_WAV, new MyAudioRecorder.OnAudioDataCallback() {
//            @Override
//            public void onBeginRecorder() {
//                Log.d(TAG, "onBeginRecorder");
//            }
//
//            @Override
//            public void onRecordStop() {
//                Log.d(TAG, "onRecordStop");
//            }
//
//            @Override
//            public void onCancel() {
//                Log.d(TAG, "onCancel");
//            }
//
//            @Override
//            public void onCancelQuiet() {
//                Log.d(TAG, "onCancelQuiet");
//            }
//
//            @Override
//            public void onAudioData(byte[] var1, int var2) {
//                Log.d(TAG, "onAudioData");
//            }
//
//            @Override
//            public void onError(int var1, String var2) {
//                Log.d(TAG, "onError");
//            }
//        });

//        StreamAudioRecorder.getInstance().start(getCachePath("xs") + "/xs_1" + ".wav", new StreamAudioRecorder.AudioStartCompeletedCallback() {
//            @Override
//            public void onAudioStartCompeleted() {
//                Log.d(TAG,"onAudioStartCompeleted");
//            }
//        }, new StreamAudioRecorder.AudioStopCompletedCallback() {
//            @Override
//            public void onAudioStopCompeleted() {
//                Log.d(TAG,"onAudioStopCompeleted");
//            }
//        }, new StreamAudioRecorder.AudioDataCallback() {
//            @Override
//            public void onAudioData(byte[] data, int size, AtomicBoolean recording, int flag) {
//                Log.d(TAG,"onAudioData-data:"+data+" size:"+size+" recording:"+recording+" flag:"+flag);
//            }
//
//            @Override
//            public void onError(int code) {
//                Log.d(TAG,"onError-code:"+code);
//
//            }
//        });
//        StreamAudioPlayer.getInstance().play(getCachePath("xs") + "/xs_1" + ".wav", new StreamAudioPlayer.AudioPlayCompeletedCallback() {
//            @Override
//            public void onAudioPlayCompeleted() {
//                Log.d(TAG, "onAudioPlayCompeleted");
//            }
//        });
//        try {
//            mp3Recorder.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            MyAACUtils.aacFile = aacFile;
//            myAACRecord.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        checkRecordPermission();
        startRecord();
    }

//    File file = new File(getCachePath("xs") + "/xs_1" + ".mp3");
//    MP3Recorder mp3Recorder = new MP3Recorder(file);

//    String aacFile = getCachePath("xs") + "/xs_1" + ".aac";
//    MyAACRecord myAACRecord = new MyAACRecord(new File(getCachePath("xs") + "/xs_1" + ".mp3"));

    public void onStop(View view) {
//        MyAudioRecorder.getInstance().stop();
//        StreamAudioRecorder.getInstance().stop();
//        StreamAudioPlayer.getInstance().stopPlay();
//        mp3Recorder.stop();
//        myAACRecord.stop();
        stopRecord();
    }


    private void startRecord() {
        mAudioRecorder.start();
        mAudioEncorder.start();
    }

    private void stopRecord() {
        mAudioRecorder.stopRecording();
        mAudioEncorder.stopEncording();
    }

    /**
     * 录音线程
     */
    public class AudioRecorder extends Thread {

        private AudioRecord mAudioRecord;
        private boolean isRecording;
        private int minBufferSize;

        public AudioRecorder() {
            isRecording = true;
            initRecorder();
        }

        @Override
        public void run() {
            super.run();
            startRecording();
        }

        /**
         * 初始化录音
         */
        public void initRecorder() {
            minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, minBufferSize);
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                isRecording = false;
                Log.e(TAG, "state:record-STATE_INITIALIZED");
                return;
            }
        }

        /**
         * 释放资源
         */
        public void release() {
            if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioRecord.stop();
            }
        }

        /**
         * 开始录音
         */
        public void startRecording() {
            if (mAudioRecord == null) {
                return;
            }

            mAudioRecord.startRecording();
            while (isRecording) {
                AudioDate audioDate = new AudioDate();
                audioDate.buffer = ByteBuffer.allocateDirect(minBufferSize);
                audioDate.size = mAudioRecord.read(audioDate.buffer, minBufferSize);
                try {
                    if (queue != null) {
                        queue.put(audioDate);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            release();
        }

        /**
         * 结束录音
         */
        public void stopRecording() {
            isRecording = false;
        }
    }

    /**
     * 音频编码线程
     */
    public class AudioEncorder extends Thread {

        private MediaCodec mEncorder;
        private Boolean isEncording = false;
        private int minBufferSize;

        private OutputStream mFileStream;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        public AudioEncorder() {
            isEncording = true;
            initEncorder();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            startEncording();
        }

        /**
         * 初始化编码器
         */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        private void initEncorder() {

            minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            try {
                mEncorder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, channelConfig);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            format.setInteger(MediaFormat.KEY_BIT_RATE, sampleRateInHz * 16 * 1);// sampleRateInHz * bit * channelCount
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
            mEncorder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }

        /**
         * 开始编码
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void startEncording() {
            if (mEncorder == null) {
                return;
            }
            mEncorder.start();
            try {
//                mFileStream = new FileOutputStream(getSDPath() + "/aac_encode.aac");
                mFileStream = new FileOutputStream(getCachePath("xs") + "/xs_1" + ".aac");
                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                AudioDate audioDate;
                while (isEncording) {
                    // 从队列中取出录音的一帧音频数据
                    audioDate = getAudioDate();
                    if (audioDate == null) {
                        continue;
                    }
                    // 取出InputBuffer，填充音频数据，然后输送到编码器进行编码
//                    int inputBufferIndex = mEncorder.dequeueInputBuffer(0);//可能丢帧
                    int inputBufferIndex = mEncorder.dequeueInputBuffer(-1);// 防止丢帧
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
         * 停止编码
         */
        public void stopEncording() {
            isEncording = false;
        }

        /**
         * 从队列中取出一帧待编码的音频数据
         *
         * @return
         */
        public AudioDate getAudioDate() {
            if (queue != null) {
                try {
                    return (AudioDate) queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        /**
         * 添加ADTS头
         *
         * @param packet
         * @param packetLen
         */
        private void addADTStoPacket(byte[] packet, int packetLen) {
            int profile = 2; //AAC LC
            int freqIdx = 4; //44100 根据不同的采样率修改这个值
//            int chanCfg = 2; //CPE
            int chanCfg = 1; //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
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
    }

    public String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    public void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MICROPHONE, 1);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 1);
            return;
        }
    }

}
