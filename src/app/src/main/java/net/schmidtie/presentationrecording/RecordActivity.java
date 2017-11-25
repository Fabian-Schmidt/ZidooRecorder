package net.schmidtie.presentationrecording;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class RecordActivity extends Activity {
    private static final String TAG = "RecordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_record);

        initService();
    }

    private RecorderServiceBindTool mRecorderServiceBindTool = null;
    private RecorderClass mRecorderClass;
    private IHdmiStateChange StateChange = new IHdmiStateChange() {
        private final int TOAST_DURATION = Toast.LENGTH_SHORT;

        @Override
        public void StartRecording() {
            Toast.makeText(RecordActivity.this, "Start Recording", TOAST_DURATION).show();
        }

        @Override
        public void StopRecording() {
            Toast.makeText(RecordActivity.this, "Stop Recording", TOAST_DURATION).show();
        }

        @Override
        public void StartPreview() {
            View HdmiNoSignaleView = findViewById(R.id.home_ac_video_hdmi_nosignale);
            if (HdmiNoSignaleView != null) {
                HdmiNoSignaleView.setVisibility(View.GONE);
            }
        }

        @Override
        public void StopPreview() {
            View HdmiNoSignaleView = findViewById(R.id.home_ac_video_hdmi_nosignale);
            if (HdmiNoSignaleView != null) {
                HdmiNoSignaleView.setVisibility(View.VISIBLE);
            }
        }
    };

    private void initService() {
        mRecorderServiceBindTool = RecorderServiceBindTool.getInstance(this);
        mRecorderServiceBindTool.initService(new RecorderServiceBindTool.RecorderServiceListener() {

            @Override
            public void service(RecorderService recorderService) {
                RecordActivity.this.mRecorderClass = recorderService.getRecorderInterface();
                ViewGroup rootView = (ViewGroup) findViewById(R.id.home_ac_video_hdmi_textureView);
                RecordActivity.this.mRecorderClass.SetRootView(rootView);
                RecordActivity.this.mRecorderClass.SetStateChange(StateChange);
                RecordActivity.this.mRecorderClass.startDisplay();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mRecorderServiceBindTool != null) {
            mRecorderServiceBindTool.release();
        }
        if (mRecorderClass != null) {
            mRecorderClass.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (mRecorderClass != null) {
            mRecorderClass.stopDisplay();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mRecorderClass != null) {
            mRecorderClass.startDisplay();
        }
        super.onResume();
    }
}
