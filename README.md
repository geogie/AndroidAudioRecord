AndroidAudioRecordRecorder
1.  audio文件下：pcm录音，wav文件不能播放。
2.  mySound文件下：myMp3是lame进行实时pcm流--》mp3文件转码。[lameso文件来源][https://github.com/geogie/MyLame]
3. *record文件下：
    WaveHeader:wav音频文件格式
    AudioFormatConvertUtil:pcm文件--》wav文件转化
    MyAudioRecorder:考虑多线程编程，录音--》pcm文件--》wav文件
    StreamAudioRecorder：考虑多线程编程，录音--》pcm流--》wav流生成文件
    StreamAudioPlayer：考虑多线程编程，用AudioTrack，音频文件流--》实时播放音频*