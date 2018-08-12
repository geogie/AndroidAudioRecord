package com.example.george.androidaudiorecord.audio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.george.androidaudiorecord.MyApp;
import com.example.george.androidaudiorecord.R;
import com.example.george.androidaudiorecord.utils.Logger;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AudioRecordAACActivity extends AppCompatActivity {
    private static final String TAG = AudioRecordAACActivity.class.getSimpleName();

    @BindView(R.id.btRecord)
    Button btRecord;
    @BindView(R.id.btStop)
    Button btStop;
    @BindView(R.id.tvState)
    TextView tvState;
    @BindView(R.id.tvSoundSize)
    TextView tvSoundSize;
    private boolean isStart = false;
    private boolean isPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        ButterKnife.bind(this);

        RecordManager.getInstance().init(MyApp.getInstance(), true);
        RecordManager.getInstance().changeFormat(RecordConfig.RecordFormat.WAV);
        RecordManager.getInstance().setRecordStateListener(new RecordStateListener() {
            @Override
            public void onStateChange(RecordHelper.RecordState state) {
                Logger.i(TAG, "onStateChange %s", state.name());

                switch (state) {
                    case PAUSE:
                        tvState.setText("暂停中");
                        break;
                    case IDLE:
                        tvState.setText("空闲中");
                        break;
                    case RECORDING:
                        tvState.setText("录音中");
                        break;
                    case STOP:
                        tvState.setText("停止");
                        break;
                    case FINISH:
                        tvState.setText("录音结束");
                        tvSoundSize.setText("---");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(String error) {
                Logger.i(TAG, "onError %s", error);
            }
        });
        RecordManager.getInstance().setRecordSoundSizeListener(new RecordSoundSizeListener() {
            @Override
            public void onSoundSize(int soundSize) {
                tvSoundSize.setText(String.format(Locale.getDefault(), "声音大小：%s db", soundSize));
            }
        });
    }

    @OnClick({R.id.btRecord, R.id.btStop})
    public void onViewClicked(View view) {//
        switch (view.getId()) {
            case R.id.btRecord:
                if (isStart) {
                    RecordManager.getInstance().pasue();// 暂停录音
                    btRecord.setText("开始");
                    isPause = true;
                    isStart = false;
                } else {
                    if (isPause) {
                        RecordManager.getInstance().resume();// 开始录音（和pause对应）
                    } else {
                        RecordManager.getInstance().start();//开始录音（和stop对应）
                    }
                    btRecord.setText("暂停");
                    isStart = true;
                }

                break;
            case R.id.btStop:
                RecordManager.getInstance().stop();
                btRecord.setText("开始");
                isPause = false;
                isStart = false;
                break;
            default:
                break;
        }
    }
}
