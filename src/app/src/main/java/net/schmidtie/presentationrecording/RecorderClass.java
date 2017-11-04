package net.schmidtie.presentationrecording;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.realtek.hardware.RtkHDMIRxManager;
import com.realtek.server.HDMIRxParameters;
import com.realtek.server.HDMIRxStatus;

import net.schmidtie.presentationrecording.info.ResolutionInfo;
import net.schmidtie.util.FileUtil;
import net.schmidtie.util.Hdmi;
import net.schmidtie.util.Tuple;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

    private final static int MESSAGE_DISPLAY = 0;
    private final static int MESSAGE_HDMI_CHANGED = 1;
    private final static int MESSAGE_START_RECORD = 2;
    private final static int MESSAGE_TIMER = 3;
    private final static int MESSAGE_TIMER_TIME = 5000;
    private final static int MESSAGE_START_RECORD_DELAY = 50;
    private final static int MESSAGE_RETRY_TIME = 200;

    private final static long MAX_FILE_SIZE = 1536L * 1024L * 1024L;


    private IHdmiStateChange StateChange;

    RecorderClass(Context mContext) {
        this.mContext = mContext;
        this.HdmiViewType = HdmiDisplayType.TEXTUREVIEW;
        this.DesireState.HdmiVideo = true;

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
                                && DesireState.HdmiVideoRecording) {
                            startRecord();
                            if (StateChange != null) {
                                StateChange.StartRecording();
                            }
                            CurrentState.HdmiRecordTime = 0;
                            MessageHandler.sendEmptyMessageDelayed(MESSAGE_TIMER, MESSAGE_TIMER_TIME);
                        }
                        break;
                    }

                    case MESSAGE_TIMER: {
                        if (HdmiIsConnect
                                && CurrentState.HdmiVideo
                                && CurrentState.HdmiVideoRecording) {
                            CurrentState.HdmiRecordTime = CurrentState.HdmiRecordTime + 1;

                            try {
                                String str = CurrentState.HdmiRecordPath + File.separator + CurrentState.HdmiRecordFile;
                                File file = new File(str);
                                long fileSize = file.length();
                                if (fileSize > MAX_FILE_SIZE) {
                                    if (StateChange != null) {
                                        StateChange.StopRecording();
                                    }
                                    stopRecord();
                                    MessageHandler.sendEmptyMessageDelayed(MESSAGE_START_RECORD, MESSAGE_START_RECORD_DELAY);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Failed restarting recording.", ex);
                            }

                            MessageHandler.sendEmptyMessageDelayed(MESSAGE_TIMER, MESSAGE_TIMER_TIME);
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
    //private WindowManager mWindowManager = null;
    //private FloatingWindowView mFloatingView = null;

    private void initView() {
        if (this.RootView != null) {
            //this.mWindowManager = (WindowManager) this.mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            //LayoutInflater li = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //this.mFloatingView = (FloatingWindowView) li.inflate(R.layout.floating_window, null);
            //RelativeLayout viewGroup = (RelativeLayout) this.mFloatingView.findViewById(R.id.floating_surfaceview);

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
        /*WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
*/
       /* WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN;*/
       /* WindowManager.LayoutParams params = new WindowManager.LayoutParams(720, 480, 2005, 2568, -3);
        params.x = 0;
        params.y = 0;
        //params.gravity = 51;
        //this.mFloatingView.setBackgroundColor(0xff000000);
        this.mFloatingView.setBackgroundColor(0xff0ff000);
        try {
            this.mFloatingView.setSystemUiVisibility(5894);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mWindowManager.addView(this.mFloatingView, params);*/
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

                //mFloatingView.setHdmiDisPlay(false);
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
                Tuple<Integer, Integer> previewSize = getSupportedPreviewSize(hdmirxGetParam, rxStatus.width, rxStatus.height);
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
                        // TextureView.setRotation(180);
                        HDMIRxManager.setPreviewDisplay3(surfaceTexture);
                    }
                    // configureTargetFormat
                    HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                    Log.d(TAG, "setPreviewSize Width = " + DesireState.Width + " Height = " + DesireState.Height);
                    hdmirxParam.setPreviewSize(DesireState.Width, DesireState.Height);
                    int fps = DesireState.Fps;
                    if (fps == 60) {
                        fps = 30;
                    }
                    if (fps == 50) {
                        fps = 25;
                    }
                    Log.d(TAG, "setPreviewFrameRate Fps = " + fps);
                    hdmirxParam.setPreviewFrameRate(fps);
                    // set source format
                    HDMIRxManager.setParameters(hdmirxParam);
                    CurrentState.Width = DesireState.Width;
                    CurrentState.Height = DesireState.Height;
                    CurrentState.Fps = fps;
                    //HDMIRxManager.configureTargetFormat(new RtkHDMIRxManager.VideoConfig(1920, 1080, 10000000), new RtkHDMIRxManager.AudioConfig(2, 48000, 32000));
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
            if (previewSizes.get(i) != null && rxWidth == previewSizes.get(i).width) {
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
                int width = 1920;
                int height = 1080;
                //RecordInfo currentRecordInfo = getCurrentRecordInfo();
                //ResolutionInfo resolution = getResolution(currentRecordInfo.mResolution == 0 ? 1 : currentRecordInfo.mResolution);
                int intValue = 2;
                int intValue2 = 48000;
                int i = 441;
                if (intValue2 % 8000 == 0) {
                    i = 480;
                }
                i = ((intValue * 640) * intValue2) / i;
                Log.d(TAG, "isContinueRecorder = " + "   w = " + width + "  h = " + height);
                int bitRate = 15000000;
                RtkHDMIRxManager.VideoConfig videoConfig = new RtkHDMIRxManager.VideoConfig(width, height, bitRate);
                Log.d(TAG, "AudioConfig channelCount = " + intValue + "   sampleRate = " + intValue2 + "  audioBitrate = " + i);

                RtkHDMIRxManager.AudioConfig audioConfig = new RtkHDMIRxManager.AudioConfig(intValue, intValue2, i);
                HDMIRxManager.configureTargetFormat(videoConfig, audioConfig);

                int FileFormat = RtkHDMIRxManager.HDMIRX_FILE_FORMAT_TS;
                String recordFolder = FileUtil.GetStorageFolder();
                if (recordFolder == null)
                    recordFolder = "/storage/emulated/0";
                recordFolder = recordFolder + File.separator + "hdmi";
                CurrentState.HdmiRecordPath = recordFolder;
                Random r = new Random();
                int rndNumber = r.nextInt(999999);
                String recordFileName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + rndNumber + (FileFormat == RtkHDMIRxManager.HDMIRX_FILE_FORMAT_TS ? ".ts" : ".mp4");
                CurrentState.HdmiRecordFile = recordFileName;
                String str = recordFolder + File.separator + recordFileName;
                Log.d(TAG, "record path = " + str);
                //FileUtil.CreateFileWithFullAcess(recordFolder, recordFileName);
                File file = new File(str);
                file.createNewFile();
                FileUtil.ChangeFileToFullAcess(str);

                HDMIRxManager.setTargetFd(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_APPEND | ParcelFileDescriptor.MODE_WRITE_ONLY), FileFormat);//939524096
                HDMIRxManager.setTranscode(true);

                CurrentState.HdmiVideoRecording = true;

                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Error during start recording", ex);
                CurrentState.HdmiVideoRecording = false;
            }
        }
        return false;
    }

    public boolean stopRecord() {
        Log.d(TAG, "stopRecord");
        try {
            if (HDMIRxManager != null) {
                HDMIRxManager.setTranscode(false);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error during stopRecord", ex);
        }
        CurrentState.HdmiVideoRecording = false;
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
        DesireState.HdmiVideoRecording = true;
        startPreview();
        return false;
    }

    boolean stopDisplay() {
        DesireState.HdmiVideo = false;
        DesireState.HdmiVideoRecording = false;
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
