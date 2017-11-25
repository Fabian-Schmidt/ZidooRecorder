package net.schmidtie.presentationrecording;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import net.schmidtie.presentationrecording.info.ResolutionInfo;
import net.schmidtie.util.Hdmi;
import net.schmidtie.util.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class RecorderClass {
    private static final String TAG = "RecorderClass";

    private DisplayState DesireState = new DisplayState();
    private DisplayState CurrentState = new DisplayState();

    private boolean PreviewSurfaceAvailable = false;
    private boolean HdmiIsConnect = false;

    private Context mContext = null;
    private ViewGroup RootView = null;
    private HdmiDisplayType HdmiViewType = HdmiDisplayType.TEXTUREVIEW;

    private BroadcastReceiver HdmiRxHotPlugReceiver = null;
    private RtkHDMIRxManager HDMIRxManager = null;
    private Handler MessageHandler = null;
    private OutputWriter mOutputWriter = null;

    private final static int MESSAGE_DISPLAY = 0;
    private final static int MESSAGE_HDMI_CHANGED = 1;
    private final static int MESSAGE_START_RECORD = 2;
    private final static int MESSAGE_TIMER = 3;
    private final static int MESSAGE_TIMER_TIME = 5000;
    private final static int MESSAGE_START_RECORD_DELAY = 50;
    private final static int MESSAGE_RETRY_TIME = 200;

    private IHdmiStateChange StateChange;

    RecorderClass(Context mContext) {
        this.mContext = mContext;
        this.HdmiViewType = HdmiDisplayType.TEXTUREVIEW;
        this.DesireState.HdmiVideo = true;

        Settings settings = SettingsActivity.ReadSettings(mContext);
        Log.d(TAG, settings.toString());

        DesireState.HdmiVideoRecording = settings.RecordLocal;
        DesireState.HdmiRecordToDeviceAllowed = settings.RecordAllowInternal;

        DesireState.HdmiVideoStream = settings.StreamUDP;
        DesireState.UDP_Target_IP = settings.StreamUDP_IP;
        DesireState.UDP_Target_Port = settings.StreamUDP_Port;

        DesireState.VideoBitrate = settings.QualityVideoBitrate;
        DesireState.VideoReduceFramerate = settings.QualityVideoReduceFramerate;
        DesireState.VideoLimitResolution = settings.QualityVideoLimitResolution;
        DesireState.AudioSamples = settings.QualityAudioSamples;
        init();
    }

    public void SetRootView(ViewGroup rootView) {
        this.RootView = rootView;
        initView();
    }

    public void SetStateChange(IHdmiStateChange stateChange) {
        this.StateChange = stateChange;
    }

    private void init() {
        mOutputWriter = new OutputWriter(this.mContext, this);
        initHDMIRxManager();
        initView();
        MessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String MsgType = "Unknown";
                switch (msg.what) {
                    case MESSAGE_DISPLAY:
                        MsgType = "Display";
                        break;
                    case MESSAGE_HDMI_CHANGED:
                        MsgType = "HDMI changed";
                        break;
                    case MESSAGE_START_RECORD:
                        MsgType = "Start recoding";
                        break;
                    case MESSAGE_TIMER:
                        MsgType = "Timer";
                        break;
                }
                Log.i(TAG, "handleMessage: " + MsgType);
                switch (msg.what) {
                    case MESSAGE_DISPLAY:
                    case MESSAGE_HDMI_CHANGED: {
                        MessageHandler.removeMessages(MESSAGE_DISPLAY);
                        MessageHandler.removeMessages(MESSAGE_HDMI_CHANGED);
                        MessageHandler.removeMessages(MESSAGE_START_RECORD);
                        MessageHandler.removeMessages(MESSAGE_TIMER);
                        if (HdmiIsConnect && DesireState.HdmiVideo) {
                            if (StateChange != null)
                                StateChange.StartPreview();
                            boolean previewIsRunning = startPreview();
                            if (previewIsRunning) {
                                MessageHandler.sendEmptyMessageDelayed(MESSAGE_START_RECORD, MESSAGE_START_RECORD_DELAY);
                            } else {
                                MessageHandler.sendEmptyMessageDelayed(MESSAGE_DISPLAY, MESSAGE_RETRY_TIME);
                            }
                        } else {
                            if (StateChange != null)
                                StateChange.StopPreview();
                            if (StateChange != null && CurrentState.HdmiVideoRecording) {
                                StateChange.StopRecording();
                            }
                            // Always call Stop Recording to ensure correct state.
                            stopRecord();
                            stopPreview();
                        }
                        break;
                    }

                    case MESSAGE_START_RECORD: {
                        MessageHandler.removeMessages(MESSAGE_TIMER);
                        if (HdmiIsConnect
                                && CurrentState.HdmiVideo
                                && (DesireState.HdmiVideoRecording || DesireState.HdmiVideoStream)) {
                            startRecord();
                            if (StateChange != null) {
                                StateChange.StartRecording();
                            }
                            mOutputWriter.StartWritingOutput();
                            CurrentState.HdmiRecordTime = 0;
                            MessageHandler.sendEmptyMessageDelayed(MESSAGE_TIMER, MESSAGE_TIMER_TIME);
                        }
                        break;
                    }

                    case MESSAGE_TIMER: {
                        if (HdmiIsConnect
                                && CurrentState.HdmiVideo
                                && (CurrentState.HdmiVideoRecording || CurrentState.HdmiVideoStream)) {
                            CurrentState.HdmiRecordTime = CurrentState.HdmiRecordTime + 1;
                            //MessageHandler.sendEmptyMessageDelayed(MESSAGE_TIMER, MESSAGE_TIMER_TIME);
                        }
                    }
                    default:
                        break;
                }
            }
        };
        initHdmiHotplugDetection();
    }

    private void initHDMIRxManager() {
        HDMIRxManager = new RtkHDMIRxManager();
        HDMIRxManager.setListener(new MyHdmiListener());

        Log.d(TAG, "HDMIRxManager.setPlayback videoEn = " + DesireState.HdmiVideo + "  audioEn = " + DesireState.HdmiAudio);
        HDMIRxManager.setPlayback(DesireState.HdmiVideo, DesireState.HdmiAudio);
    }

    private void initHdmiHotplugDetection() {
        HdmiRxHotPlugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                HdmiIsConnect = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
                Log.d(TAG, "Intent HDMIRX_PLUGGED HDMIRX_PLUGGED_STATE = " + HdmiIsConnect);
                MessageHandler.sendEmptyMessage(MESSAGE_HDMI_CHANGED);
            }
        };

        if (HDMIRxManager != null) {
            HdmiIsConnect = HDMIRxManager.isHDMIRxPlugged();
        }
        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        mContext.registerReceiver(HdmiRxHotPlugReceiver, hdmiRxFilter);
    }

    private void unregisterHdmiHotplugDetection() {
        mContext.unregisterReceiver(HdmiRxHotPlugReceiver);
    }

    private View PreviewView = null;
    private TextureView TextureView = null;
    private SurfaceView SurfaceView = null;
    private SurfaceHolder SurfaceViewHolder = null;
    private FloatingWindowSurfaceCallback SurfaceViewCallback = null;

    private void initView() {
        if (this.RootView != null) {
            // setup view type
            if (HdmiViewType == HdmiDisplayType.SURFACEVIEW) {
                SurfaceView = new SurfaceView(mContext);
                SurfaceViewCallback = new FloatingWindowSurfaceCallback();
                SurfaceViewHolder = SurfaceView.getHolder();
                SurfaceViewHolder.addCallback(SurfaceViewCallback);
                PreviewView = SurfaceView;
            } else if (HdmiViewType == HdmiDisplayType.TEXTUREVIEW) {
                TextureView = new TextureView(mContext);
                FloatingWindowTextureListener TextureViewListener = new FloatingWindowTextureListener();
                TextureView.setSurfaceTextureListener(TextureViewListener);
                PreviewView = TextureView;
            }

            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            PreviewView.setLayoutParams(param);
            this.RootView.addView(PreviewView);
        }
    }

    private boolean stopPreview() {
        if (PreviewView != null) {
            PreviewView.setVisibility(View.INVISIBLE);
        }

        boolean rlt = true;
        if (HDMIRxManager != null) {
            try {
                Log.d(TAG, "HDMIRxManager.setPlayback videoEn = " + false + "  audioEn = " + false);
                HDMIRxManager.setPlayback(false, false);

                HDMIRxManager.stop();
                HDMIRxManager.release();
            } catch (Exception ex) {
                Log.e(TAG, "Error during stopPreview", ex);
            }
        } else {
            rlt = false;
        }
        CurrentState.HdmiVideo = false;
        CurrentState.HdmiAudio = false;
        CurrentState.Fps = 0;
        CurrentState.Width = 0;
        CurrentState.Height = 0;
        return rlt;
    }

    private class MyHdmiListener implements RtkHDMIRxManager.HDMIRxListener {
        @Override
        public void onEvent(int i) {
            Log.d(TAG, "RtkHDMIRxManager.HDMIRxListener: " + String.valueOf(i));
        }
    }

    private boolean startPreview() {
        Log.d(TAG, "startPreview");
        if (PreviewView == null) {
            return false;
        }
        PreviewView.setVisibility(View.VISIBLE);
        Log.d(TAG, "PreviewSurfaceAvailable = " + PreviewSurfaceAvailable + " CurrentState.HdmiVideo = " + CurrentState.HdmiVideo);
        if (PreviewSurfaceAvailable && !CurrentState.HdmiVideo) {
            if (HDMIRxManager == null) {
                initHDMIRxManager();
                //Log.e(TAG, "startPreview.HDMIRxManager == null");
            }
            HDMIRxStatus rxStatus = HDMIRxManager.getHDMIRxStatus();
            if (rxStatus == null || rxStatus.status != HDMIRxStatus.STATUS_READY) {
                return false;
            } else {
                if (HDMIRxManager.open() != 0) {
                    CurrentState.Fps = 0;
                    CurrentState.Width = 0;
                    CurrentState.Height = 0;
                    CurrentState.HdmiVideo = false;
                    try {
                        HDMIRxManager.stop();
                        HDMIRxManager.release();
                    } catch (Exception ex) {
                        Log.i(TAG, "Error stopping other HDMIRx process.");
                    }
                    HDMIRxManager = null;
                    return false;
                }
                HDMIRxParameters hdmirxGetParam = HDMIRxManager.getParameters();
                DesireState.Width = rxStatus.width;
                DesireState.Height = rxStatus.height;
                if (CurrentState.VideoLimitResolution && (DesireState.Width > 1920 || DesireState.Height > 1080)) {
                    DesireState.Width = 1920;
                    DesireState.Height = 1080;
                }
                Tuple<Integer, Integer> previewSize = getSupportedPreviewSize(hdmirxGetParam, DesireState.Width, DesireState.Height);
                DesireState.Width = previewSize.x;
                DesireState.Height = previewSize.y;
                DesireState.Fps = getSupportedPreviewFrameRate(hdmirxGetParam);
                // mScanMode = rxStatus.scanMode;

                try {
                    Log.d(TAG, "setPreviewDisplay HdmiViewType = " + HdmiViewType);
                    if (HdmiViewType == HdmiDisplayType.SURFACEVIEW) {
                        HDMIRxManager.setPreviewDisplay(SurfaceViewHolder);
                    } else if (HdmiViewType == HdmiDisplayType.TEXTUREVIEW) {
                        SurfaceTexture surfaceTexture = TextureView.getSurfaceTexture();
                        HDMIRxManager.setPreviewDisplay3(surfaceTexture);
                    }
                    // configureTargetFormat
                    HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                    Log.d(TAG, "setPreviewSize Width = " + DesireState.Width + " Height = " + DesireState.Height);
                    //Check output resolution? For better 4K downscaling?
                    hdmirxParam.setPreviewSize(DesireState.Width, DesireState.Height);
                    int fps = DesireState.Fps;
                    if (this.DesireState.VideoReduceFramerate) {
                        if (fps == 60) {
                            fps = 30;
                        }
                        if (fps == 50) {
                            fps = 25;
                        }
                    }
                    Log.d(TAG, "setPreviewFrameRate Fps = " + fps);
                    hdmirxParam.setPreviewFrameRate(fps);
                    // set source format
                    HDMIRxManager.setParameters(hdmirxParam);
                    CurrentState.Width = DesireState.Width;
                    CurrentState.Height = DesireState.Height;
                    CurrentState.Fps = fps;
                    CurrentState.VideoReduceFramerate = DesireState.VideoReduceFramerate;
                    CurrentState.VideoLimitResolution = DesireState.VideoLimitResolution;
                    // configureTargetFormat end
                    HDMIRxManager.play();
                    CurrentState.HdmiVideo = true;
                    Log.d(TAG, "Preview successful");
                    return true;
                } catch (Exception e) {
                    stopPreview();
                    e.printStackTrace();
                    Log.e(TAG, "startPreview error " + e.getMessage(), e);
                    return false;
                }
            }
        } else if (!PreviewSurfaceAvailable) {
            return false;
        } else {
            return false;
        }
    }

    private static int getSupportedPreviewFrameRate(HDMIRxParameters hdmirxGetParam) {
        List<Integer> previewFrameRates = hdmirxGetParam.getSupportedPreviewFrameRates();
        int fps = 0;
        if (previewFrameRates != null && previewFrameRates.size() > 0)
            fps = previewFrameRates.get(previewFrameRates.size() - 1);
        else
            fps = 30;
        return fps;
    }

    private static Tuple<Integer, Integer> getSupportedPreviewSize(HDMIRxParameters hdmirxGetParam, int rxWidth, int rxHeight) {
        List<com.realtek.hardware.RtkHDMIRxManager.Size> previewSizes = hdmirxGetParam.getSupportedPreviewSizes();
        int retWidth = 0, retHeight = 0;
        if (previewSizes == null || previewSizes.size() <= 0)
            return null;

        for (int i = 0; i < previewSizes.size(); i++) {
            com.realtek.hardware.RtkHDMIRxManager.Size size = previewSizes.get(i);
            Log.i(TAG, "SupportedPreviewSize(" + i + ") width = " + size.width + " height = " + size.height);
        }

        for (int i = 0; i < previewSizes.size(); i++) {
            if (rxWidth == previewSizes.get(i).width) {
                retWidth = previewSizes.get(i).width;
                retHeight = previewSizes.get(i).height;
                if (rxHeight == previewSizes.get(i).height)
                    break;
            }
        }
        if (retWidth == 0 && retHeight == 0) {
            if (previewSizes.get(previewSizes.size() - 1) != null) {
                retWidth = previewSizes.get(previewSizes.size() - 1).width;
                retHeight = previewSizes.get(previewSizes.size() - 1).height;
            }
        }

        return new Tuple<Integer, Integer>(retWidth, retHeight);
    }

    private ArrayList<ResolutionInfo> mResolutionInfoList = new ArrayList<ResolutionInfo>();
/*
    private void initResolution() {
        this.mResolutionInfoList.clear();
        this.mResolutionInfoList.add(new ResolutionInfo());
        if (HdmiIsConnect) {
            Camera defaultCameraInstance = CameraHelper.getDefaultCameraInstance();
            List supportedPreviewSizes = defaultCameraInstance.getParameters().getSupportedPreviewSizes();
            if (supportedPreviewSizes != null) {
                Collection arrayList = new ArrayList();
                int size = supportedPreviewSizes.size() - 1;
                while (size > 0) {
                    try {
                        Size size2 = (Size) supportedPreviewSizes.get(size);
                        MyLog.m17v("setHdmiConnectText Size = " + size2.width + " * " + size2.height);
                        if (size2.width <= 1920) {
                            ResolutionInfo resolutionInfo = new ResolutionInfo(size2.width, size2.height);
                            if (resolutionInfo.mWidth == 3840 && resolutionInfo.mHeight == 2160) {
                                this.mResolutionInfoList.add(resolutionInfo);
                            } else if (resolutionInfo.mWidth == 1920 && resolutionInfo.mHeight == 1080) {
                                this.mResolutionInfoList.add(resolutionInfo);
                            } else if (resolutionInfo.mWidth == 1280 && resolutionInfo.mHeight == 720) {
                                this.mResolutionInfoList.add(resolutionInfo);
                            } else {
                                arrayList.add(resolutionInfo);
                            }
                        }
                        size--;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                this.mResolutionInfoList.addAll(arrayList);
            }
            defaultCameraInstance.release();
        }
    }*/

    public ResolutionInfo getResolution(int i) {
        int size = mResolutionInfoList.size();
        if (i >= 0 && i < size) {
            return (ResolutionInfo) mResolutionInfoList.get(i);
        }
        return new ResolutionInfo();
    }

    public ArrayList<ResolutionInfo> getResolution() {
        if (mResolutionInfoList.size() == 0) {
            mResolutionInfoList.add(new ResolutionInfo());
        }
        return mResolutionInfoList;
    }

    public boolean startRecord() {
        Log.d(TAG, "startRecord");
        if (HDMIRxManager != null && CurrentState.HdmiVideo && !Hdmi.isHdcp()) {
            try {
                DesireState.HdmiRecordPath = null;
                File[] RecordLocations = mContext.getExternalFilesDirs(null);
                Log.d(TAG, "Possible Record Locations:" + RecordLocations.length);
                long bestRecordLocation = -1;
                for (int rli = 0; rli < RecordLocations.length; rli++) {
                    File RecordLocation = RecordLocations[rli];
                    if (DesireState.HdmiRecordToDeviceAllowed == false
                            && RecordLocation.getAbsolutePath().contains("/0/")) {
                        Log.d(TAG, "Record Location " + rli + " is local (excluded).");
                    } else {
                        long RecordLocationFreeSpace = RecordLocation.getFreeSpace();
                        if (RecordLocationFreeSpace > bestRecordLocation) {
                            DesireState.HdmiRecordPath = RecordLocation;
                        }
                        Log.d(TAG, "Record Location " + rli + ": Absolute Path " + RecordLocation.getAbsolutePath());
                        Log.d(TAG, "Record Location " + rli + ": Free Space " + RecordLocationFreeSpace + " bytes");
                    }
                }

                ParcelFileDescriptor prepareIO = mOutputWriter.prepareIO(this.DesireState);
                if (prepareIO == null) {
                    Log.d(TAG, "mOutputWriter.prepareIO() == null");
                    return false;
                }
                int width = CurrentState.Width;
                int height = CurrentState.Height;
                //RecordInfo currentRecordInfo = getCurrentRecordInfo();
                //ResolutionInfo resolution = getResolution(currentRecordInfo.mResolution == 0 ? 1 : currentRecordInfo.mResolution);
                int audioChannels = 2;
                int audioSamples = DesireState.AudioSamples;
                int i = 441;
                if (audioSamples % 8000 == 0) {
                    i = 480;
                }
                i = (audioChannels * 640 * audioSamples) / i;
                Log.d(TAG, "VideoConfig w = " + width + "  h = " + height + " rate = " + this.DesireState.VideoBitrate);
                RtkHDMIRxManager.VideoConfig videoConfig = new RtkHDMIRxManager.VideoConfig(width, height, this.DesireState.VideoBitrate);
                Log.d(TAG, "AudioConfig channelCount = " + audioChannels + "   sampleRate = " + audioSamples + "  audioBitrate = " + i);

                RtkHDMIRxManager.AudioConfig audioConfig = new RtkHDMIRxManager.AudioConfig(audioChannels, audioSamples, i);
                HDMIRxManager.configureTargetFormat(videoConfig, audioConfig);

                HDMIRxManager.setTargetFd(prepareIO, DesireState.HdmiFileFormat);
                HDMIRxManager.setTranscode(true);

                CurrentState.VideoBitrate = this.DesireState.VideoBitrate;
                CurrentState.HdmiRecordToDeviceAllowed = DesireState.HdmiRecordToDeviceAllowed;
                CurrentState.HdmiRecordPath = DesireState.HdmiRecordPath;
                CurrentState.HdmiFileFormat = DesireState.HdmiFileFormat;
                CurrentState.HdmiVideoRecording = DesireState.HdmiVideoRecording;
                CurrentState.HdmiVideoStream = DesireState.HdmiVideoStream;

                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Error during start recording", ex);
                CurrentState.HdmiVideoRecording = false;
                CurrentState.HdmiVideoStream = false;
            }
        }
        return false;
    }

    public boolean IsHdmiVideoRecording() {
        return CurrentState.HdmiVideoRecording || CurrentState.HdmiVideoStream;
    }

    public boolean stopRecord() {
        Log.d(TAG, "stopRecord");
        CurrentState.HdmiVideoRecording = false;
        CurrentState.HdmiVideoStream = false;
        try {
            if (HDMIRxManager != null) {
                HDMIRxManager.setTranscode(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Error during stopRecord", ex);
        }
        return true;
    }

    private class FloatingWindowTextureListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "FloatingWindowTextureListener = onSurfaceTextureAvailable");
            PreviewSurfaceAvailable = true;
            // startPreview();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "FloatingWindowTextureListener = onSurfaceTextureDestroyed");
            // stopPreview();
            PreviewSurfaceAvailable = false;
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    private class FloatingWindowSurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder arg0) {
            Log.d(TAG, "FloatingWindowSurfaceCallback = surfaceCreated");
            PreviewSurfaceAvailable = true;
            // startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder arg0) {
            Log.d(TAG, "FloatingWindowSurfaceCallback = surfaceDestroyed");
            // stopPreview();
            PreviewSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) {
        }

    }

    boolean startDisplay() {
        DesireState.HdmiVideo = true;
        startPreview();
        return false;
    }

    boolean stopDisplay() {
        DesireState.HdmiVideo = false;
        stopPreview();
        return false;
    }

    boolean onDestroy() {
        Log.d(TAG, "onDestroy");
        stopDisplay();
        if (HdmiViewType == HdmiDisplayType.SURFACEVIEW) {
            if (SurfaceView != null && SurfaceViewHolder != null && SurfaceViewCallback != null) {
                SurfaceViewHolder.removeCallback(SurfaceViewCallback);
            }
        }

        if (HdmiRxHotPlugReceiver != null) {
            mContext.unregisterReceiver(HdmiRxHotPlugReceiver);
            HdmiRxHotPlugReceiver = null;
        }

        if (HDMIRxManager != null) {
            HDMIRxManager.release();
            HDMIRxManager = null;
        }
        return true;
    }

    public boolean setAudio(boolean isOpenAudio) {
        DesireState.HdmiAudio = isOpenAudio;
        try {
            if (HDMIRxManager != null && CurrentState.HdmiVideo) {
                HDMIRxManager.setPlayback(true, DesireState.HdmiAudio);
                CurrentState.HdmiAudio = DesireState.HdmiAudio;
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Update HDMI audio failed.", e);
            e.printStackTrace();
        }
        return false;
    }
}
