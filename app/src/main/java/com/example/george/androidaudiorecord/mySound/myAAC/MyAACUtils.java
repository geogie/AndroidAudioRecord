package com.example.george.androidaudiorecord.mySound.myAAC;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

/**
 * Created by George.ren on 2018/11/6.
 * Email:1063658094@qq.com
 * Describe:
 * MediaCodec是Android4.1之后有的（api>=16）
 * <p>
 * 采样率：44100Hz
 * 取样：16bit
 * 声道：1
 * 比特率：44100*16*1
 */
public class MyAACUtils {
    private static final String TAG = "MyAACUtils";
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
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;//aac
    int KEY_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;//aac
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodec mEncoder;
    //pts时间基数
    long presentationTimeUs = 0;
    int FREQ_IDX;
    int KEY_SAMPLE_RATE;
    int KEY_CHANNEL_COUNT;
    //channel
    int CHANNEL_IN;
    //mediaFormat-bitRate
    int KEY_BIT_RATE;
    int audioFormat;
    private byte[] mFrameByte;
    ByteBuffer[] inputBuffers = null;
    ByteBuffer[] outputBuffers = null;
    public static String aacFile;
    private volatile BufferedOutputStream outputStream;

    public static MyAACUtils getInstance() {
        return MyAACUtils.MyAACUtilsHolder.INSTANCE;
    }

    private MyAACUtils() {
    }

    private static final class MyAACUtilsHolder {
        private static final MyAACUtils INSTANCE = new MyAACUtils();

        private MyAACUtilsHolder() {
        }
    }

    /**
     * @return true配置成功，false配置失败
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean prepare(int sampleRate, int channelCount, int audioFormat, int maxBufferSize) {//44100,1,AudioFormat.ENCODING_PCM_16BIT
        /*===============init param start===================*/
        KEY_SAMPLE_RATE = sampleRate;
        if (KEY_SAMPLE_RATE == 44100) {
            FREQ_IDX = 4;
        }
        KEY_CHANNEL_COUNT = channelCount;
        CHANNEL_IN = KEY_CHANNEL_COUNT == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        this.audioFormat = audioFormat;
        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            KEY_BIT_RATE = KEY_SAMPLE_RATE * (AudioFormat.ENCODING_PCM_16BIT * 8) * KEY_CHANNEL_COUNT;
        }
        int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_IN, audioFormat);
        if (minBufferSize > maxBufferSize) {
            maxBufferSize = minBufferSize;
        }
        /*===============init param end===================*/
        /*------获取codecName-start----*/
        String codecName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = null;
            try {
                mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mediaCodecList == null) {
                Log.d(TAG, "初始化失败mediaCodecList=null");
                return false;
            }else {
                Log.d(TAG, "初始化失败mediaCodecList!=null");
            }
            MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();
            for (MediaCodecInfo codecInfo : codecInfos) {
                Log.i("TAG", "codecInfo =" + codecInfo.getName());
                for (String type : codecInfo.getSupportedTypes()) {
                    if (TextUtils.equals(type, MIME_TYPE) && codecInfo.isEncoder()) {
                        codecName = codecInfo.getName();//OMX.google.aac.encoder
                        Log.d(TAG, "see1-codecName:" + codecName);
                        break;
                    }
                }
                if (null != codecName) {
                    break;
                }
            }
        } else {
            int count = MediaCodecList.getCodecCount();
            for (int i = 0; i < count; i++) {
                MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
                for (String type : mediaCodecInfo.getSupportedTypes()) {
                    if (TextUtils.equals(type, MIME_TYPE) && mediaCodecInfo.isEncoder()) {
                        codecName = mediaCodecInfo.getName();//OMX.google.aac.encoder
                        Log.d(TAG, "see2-codecName:" + codecName);
                        break;
                    }
                }

                if (null != codecName) {
                    break;
                }
            }
        }
        Log.d(TAG, "use-codecName:" + codecName);
        if (TextUtils.isEmpty(codecName)) {
            Log.e(TAG, "codecName为空");
            return false;
        }
        /*------获取codecName-end----*/

        try {
            mBufferInfo = new MediaCodec.BufferInfo();
            mEncoder = MediaCodec.createByCodecName(codecName);
            //设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
            mediaFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE);
            //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
            /**LC-AAC,HE-AAC,HE-AACv2三种主要的编码，LC-AAC就是比较传统的AAC，相对而言，主要用于中高码率(>=80Kbps)，
             HE-AAC(相当于AAC+SBR)主要用于中低码(<=80Kbps)，
             而新近推出的HE-AACv2(相当于AAC+SBR+PS)主要用于低码率(<=48Kbps）,
             事实上大部分编码器设成<=48Kbps自动启用PS技术，而>48Kbps就不加PS,就相当于普通的HE-AAC。*/
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, KEY_AAC_PROFILE);
            //传入的数据大小
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize * 2);
            int profile = KEY_AAC_PROFILE;  //AAC LC
            int freqIdx = FREQ_IDX;  //44.1KHz
            int chanCfg = KEY_CHANNEL_COUNT;  //CPE
            ByteBuffer csd = ByteBuffer.allocate(2);
            csd.put(0, (byte) (profile << 3 | freqIdx >> 1));
            csd.put(1, (byte) ((freqIdx & 0x01) << 7 | chanCfg << 3));
            mediaFormat.setByteBuffer("csd-0", csd);
            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();// 启动MediaCodec，等待传入数据
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void release() {
        Log.d(TAG, "转码aac-release");
        inputBuffers = null;
        outputBuffers = null;
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
        }
        if (outputStream != null) {
            Log.d(TAG, "转码aac-release-关闭aac文件");
            try {
                outputStream.flush();
                outputStream.close();
                outputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "转码aac-release-关闭aac文件异常");
            }
        }
    }

    /**
     * 创建文件。
     *
     * @param f
     */
    public void touch(File f) {
        try {
            if (!f.getParentFile().exists()) {
                Log.d(TAG, "转码aac-touch-创建目录");
                f.getParentFile().mkdirs();
            }
            if (!f.exists()) {
                Log.d(TAG, "转码aac-touch-创建文件目录");
                f.createNewFile();
            } else {
                Log.d(TAG, "转码aac-touch-删除文件重新创建");
                f.delete();
                f.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "转码aac-touch-异常");
        }
    }

    public void write2Acc(byte[] bytes) {
        //写入文件中
        if (outputStream == null) {
            File aacFile = new File(this.aacFile);
            Log.d(TAG, "转码aac-write2Acc-path:");
            touch(aacFile);
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(aacFile));
                Log.e("AudioEncoder", "outputStream initialized");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            outputStream.write(bytes, 0, bytes.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "转码aac-write2Acc-写入文件异常");
        }
    }

    private long computePresentationTime(long frameIndex) {
        return frameIndex * 90000 * 1024 / KEY_SAMPLE_RATE;
    }

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
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = KEY_AAC_PROFILE;  //AAC LC
        int freqIdx = FREQ_IDX;  //44.1KHz
        int chanCfg = KEY_CHANNEL_COUNT;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private boolean codeOver = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void encode(byte[] data) {
        try {
            //api > 21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //获取一个可利用的buffer下标
                int inputBufferIndex = -1;
                try {
                    inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "inputBufferIndex:" + inputBufferIndex);
//                if (inputBufferIndex < 0) {
//                    codeOver = true;
//                    Log.d(TAG, "inputBufferIndex:" + inputBufferIndex);
//                    return;
//                }
                if (inputBufferIndex >= 0) {
                    //获取一个可利用的buffer
                    ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    //填充要编码的数据
                    inputBuffer.put(data);// pcm数据填充给inputBuffer
                    inputBuffer.limit(data.length);
                    //填充到队列中
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, computePresentationTime(presentationTimeUs), BUFFER_FLAG_CODEC_CONFIG);//通知编码器，编码
                    presentationTimeUs += 1;
                }
                //返回编码成功后buffer的下标
                int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                //循环取出编码后的数据
                while (outputBufferIndex >= 0) {
                    //获取指定下标的buffer
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);// 拿到输出buffer
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                    int length = mBufferInfo.size + 7;//给adts头字段空出7的字节，为数据添加头
                    if (mFrameByte == null || mFrameByte.length < length) {
                        mFrameByte = new byte[length];
                    }
                    addADTStoPacket(mFrameByte, length);// 添加ADTS头
                    //get数据到byte数组，向后偏移了7位
                    outputBuffer.get(mFrameByte, 7, mBufferInfo.size);
                    write2Acc(mFrameByte);
                    mFrameByte = null;
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                }
            } else {
//                int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);//其中需要注意的有dequeueInputBuffer（-1），参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
                int inputBufferIndex = -1;
                try {
                    inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (inputBufferIndex < 0) {
                    codeOver = true;
                    Log.d(TAG, "inputBufferIndex:" + inputBufferIndex);
                    return;
                }
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(data);
                    inputBuffer.limit(data.length);
                    //计算pts
                    long pts = computePresentationTime(presentationTimeUs);
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, 0);
                    presentationTimeUs += 1;
                } else {
                    Log.e(TAG, "inputBufferIndex is 0");
                }
                int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);

                while (outputBufferIndex >= 0) {
                    int outBitsSize = mBufferInfo.size;
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                    int length = mBufferInfo.size + 7;
                    if (mFrameByte == null || mFrameByte.length < length) {
                        mFrameByte = new byte[length];
                    }
                    addADTStoPacket(mFrameByte, length);
                    outputBuffer.get(mFrameByte, 7, outBitsSize);
                    outputBuffer.position(mBufferInfo.offset);
                    write2Acc(mFrameByte);
                    mFrameByte = null;
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
