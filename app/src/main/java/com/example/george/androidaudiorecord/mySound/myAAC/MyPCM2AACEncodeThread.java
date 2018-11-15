package com.example.george.androidaudiorecord.mySound.myAAC;

import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by George.ren on 2018/11/7.
 * Email:1063658094@qq.com
 * Describe:
 */
public class MyPCM2AACEncodeThread extends HandlerThread implements AudioRecord.OnRecordPositionUpdateListener {
    private static final String TAG = "MyPCM2AACEncodeThread";
    private MyPCM2AACEncodeThread.StopHandler mHandler;
    private static final int PROCESS_STOP = 1;
    private byte[] mMp3Buffer;
    private FileOutputStream mFileOutputStream;

    private static class StopHandler extends Handler {

        private MyPCM2AACEncodeThread encodeThread;

        public StopHandler(Looper looper, MyPCM2AACEncodeThread encodeThread) {
            super(looper);
            this.encodeThread = encodeThread;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PROCESS_STOP) {
                Log.d(TAG, "handleMessage-stop");
                //处理缓冲区中的数据
                while (encodeThread.processData() > 0) ;
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null);
                encodeThread.flushAndRelease();
                getLooper().quit();
            }
        }
    }

    /**
     * Constructor
     *
     * @param file       file
     * @param bufferSize bufferSize
     * @throws FileNotFoundException file not found
     */
    public MyPCM2AACEncodeThread(File file, int bufferSize) throws FileNotFoundException {
        super("DataEncodeThread");
        this.mFileOutputStream = new FileOutputStream(file);
        mMp3Buffer = new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
    }

    @Override
    public synchronized void start() {
        super.start();
        mHandler = new MyPCM2AACEncodeThread.StopHandler(getLooper(), this);
    }

    private void check() {
        if (mHandler == null) {
            throw new IllegalStateException();
        }
    }

    public void sendStopMessage() {
        check();
        mHandler.sendEmptyMessage(PROCESS_STOP);
    }

    public Handler getHandler() {
        check();
        return mHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        // Do nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        processData();
    }

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    private int processData() {
        if (mTasks.size() > 0) {
            Task task = mTasks.remove(0);
            short[] buffer = task.getData();
            int readSize = task.getReadSize();
            Log.d(TAG, "pcm2aac encode");
//            int encodedSize = LameUtil.encode(buffer, buffer, readSize, mMp3Buffer);
//            if (encodedSize > 0) {
//                try {
//                    mFileOutputStream.write(mMp3Buffer, 0, encodedSize);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                MyAACUtils.getInstance().encode(toByteArray(buffer));
            }
            return readSize;
        }else {
            Log.d(TAG,"mTasks.size<0");
        }
        return 0;
    }

    public static byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }
        return dest;
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private void flushAndRelease() {
        //将MP3结尾信息写入buffer中
        Log.d(TAG, "pcm2aac flush");
//        final int flushResult = LameUtil.flush(mMp3Buffer);
//        if (flushResult > 0) {
//            try {
//                mFileOutputStream.write(mMp3Buffer, 0, flushResult);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                if (mFileOutputStream != null) {
//                    try {
//                        mFileOutputStream.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                Log.d(TAG, "pcm2aac close");
////                LameUtil.close();
//            }
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            MyAACUtils.getInstance().release();
        }
    }

    private List<MyPCM2AACEncodeThread.Task> mTasks = Collections.synchronizedList(new ArrayList<MyPCM2AACEncodeThread.Task>());

    public void addTask(short[] rawData, int readSize) {
        mTasks.add(new MyPCM2AACEncodeThread.Task(rawData, readSize));
    }

    private class Task {
        private short[] rawData;
        private int readSize;

        public Task(short[] rawData, int readSize) {
            this.rawData = rawData.clone();
            this.readSize = readSize;
        }

        public short[] getData() {
            return rawData;
        }

        public int getReadSize() {
            return readSize;
        }
    }
}