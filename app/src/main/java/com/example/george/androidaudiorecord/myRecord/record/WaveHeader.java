package com.example.george.androidaudiorecord.myRecord.record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by George.ren on 2018/11/2.
 * Email:1063658094@qq.com
 * Describe: .wav文件头
 */
public class WaveHeader {
    public final char[] fileID = new char[]{'R', 'I', 'F', 'F'};
    public int fileLength;
    public char[] wavTag = new char[]{'W', 'A', 'V', 'E'};
    public char[] FmtHdrID = new char[]{'f', 'm', 't', ' '};
    public int FmtHdrLeth;
    public short FormatTag;
    public short Channels;
    public int SamplesPerSec;
    public int AvgBytesPerSec;
    public short BlockAlign;
    public short BitsPerSample;
    public char[] DataHdrID = new char[]{'d', 'a', 't', 'a'};
    public int DataHdrLeth;

    public WaveHeader() {
    }

    public byte[] getHeader() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.WriteChar(bos, this.fileID);
        this.WriteInt(bos, this.fileLength);
        this.WriteChar(bos, this.wavTag);
        this.WriteChar(bos, this.FmtHdrID);
        this.WriteInt(bos, this.FmtHdrLeth);
        this.WriteShort(bos, this.FormatTag);
        this.WriteShort(bos, this.Channels);
        this.WriteInt(bos, this.SamplesPerSec);
        this.WriteInt(bos, this.AvgBytesPerSec);
        this.WriteShort(bos, this.BlockAlign);
        this.WriteShort(bos, this.BitsPerSample);
        this.WriteChar(bos, this.DataHdrID);
        this.WriteInt(bos, this.DataHdrLeth);
        bos.flush();
        byte[] r = bos.toByteArray();
        bos.close();
        return r;
    }

    private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
        byte[] mybyte = new byte[]{(byte)(s << 24 >> 24), (byte)(s << 16 >> 24)};
        bos.write(mybyte);
    }

    private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
        byte[] buf = new byte[]{(byte)(n << 24 >> 24), (byte)(n << 16 >> 24), (byte)(n << 8 >> 24), (byte)(n >> 24)};
        bos.write(buf);
    }

    private void WriteChar(ByteArrayOutputStream bos, char[] id) {
        for(int i = 0; i < id.length; ++i) {
            char c = id[i];
            bos.write(c);
        }

    }
}

