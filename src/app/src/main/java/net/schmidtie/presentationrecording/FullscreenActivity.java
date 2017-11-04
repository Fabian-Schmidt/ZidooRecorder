package net.schmidtie.presentationrecording;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.schmidtie.util.FileUtil;

import java.io.File;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends Activity {
    private static final String TAG = "FullscreenActivity";

    /*private View home_ac_video_hdmi_nosignale;
    private RelativeLayout home_ac_video_hdmi_textureView;
    private boolean hdmiRxPlugged;
    private View mContentView;
    private RtkHDMIRxManager mHDMIRX;
    private SurfaceView mSurfaceView;*/

    /*
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        startActivity(new Intent(this, RecordActivity.class));
        /*try {
            String recordFolder = FileUtil.GetStorageFolder();
            //recordFolder = "/mnt/media_rw/74E6-E1B5";
            recordFolder = recordFolder + File.separator + "hdmi";
            String recordFile = "testfile.txt";
            FileUtil.CreateFileWithFullAcess(recordFolder, recordFile);
            String str = recordFolder + File.separator + recordFile;
            File file = new File(str);

            Log.d(TAG, str + " created");
        } catch (Exception ex) {
            Log.e(TAG, "Error accessing SD", ex);
        }*/
        //home_ac_video_hdmi_nosignale = findViewById(R.id.home_ac_video_hdmi_nosignale);
        //home_ac_video_hdmi_textureView = (RelativeLayout) findViewById(R.id.home_ac_video_hdmi_textureView);

        // Hide UI first
        /*ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }*/


        /*mSurfaceView = new SurfaceView(this);
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        //SurfaceViewCallback = new FloatingWindowSurfaceCallback();
        //mSurfaceHolder.addCallback(SurfaceViewCallback);
        //mPreview = mSurfaceView;
        //SurfaceView mSurfaceView = new SurfaceView(this);
        //SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        //this.SurfaceViewCallback = new FloatingWindowSurfaceCallback();
        //this.mSurfaceHolder.addCallback(this.SurfaceViewCallback);
        //this.mPreview = this.mSurfaceView;
        View mPreview = mSurfaceView;

        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPreview.setLayoutParams(param);
        home_ac_video_hdmi_textureView.addView(mPreview);

        initHdmiConnect(this);

        Log.d(TAG, "new RtkHDMIRxManager");
        mHDMIRX = new RtkHDMIRxManager();
        HDMIRxStatus hDMIRxStatus = this.mHDMIRX.getHDMIRxStatus();
        if (hDMIRxStatus == null) {
            Log.d(TAG, "No hDMIRxStatus");
        } else {
            Log.d(TAG, "hDMIRxStatus.status = " + String.valueOf(hDMIRxStatus.status));
            Log.d(TAG, "hDMIRxStatus.height = " + String.valueOf(hDMIRxStatus.height));
            Log.d(TAG, "hDMIRxStatus.width = " + String.valueOf(hDMIRxStatus.width));
            Log.d(TAG, "hDMIRxStatus.type = " + String.valueOf(hDMIRxStatus.type));
        }*/

        //Log.d(TAG, "Environment.getExternalStorageDirectory()  = " + Environment.getExternalStorageDirectory());
        //Log.d(TAG, "Environment.getExternalStorageDirectory().getAbsolutePath()  = " + Environment.getExternalStorageDirectory().getAbsolutePath());
        //Log.d(TAG, "Environment.getExternalMediaDirs()  = " + super.getExternalMediaDirs());
        //Log.d(TAG, "Environment.getExternalFilesDirs()  = " + super.getExternalFilesDirs(null));
        //Log.d(TAG, "Environment.getObbDirs()  = " + super.getObbDirs());
        //Log.d(TAG, "Environment.getExternalCacheDirs()  = " + super.getExternalCacheDirs());
        //initService();

        //verifyStoragePermissions(this);
    }



    /*private void initHdmiConnect(Context mContext) {
        BroadcastReceiver mHdmiRxHotPlugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hdmiRxPlugged = intent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
                UpdateState();
            }
        };

        IntentFilter hdmiRxFilter = new IntentFilter(HDMIRxStatus.ACTION_HDMIRX_PLUGGED);
        Intent hdmiRxPluggedIntent = mContext.registerReceiver(mHdmiRxHotPlugReceiver, hdmiRxFilter);
        hdmiRxPlugged = hdmiRxPluggedIntent.getBooleanExtra(HDMIRxStatus.EXTRA_HDMIRX_PLUGGED_STATE, false);
        //UpdateState();
    }8=*/

    /*public void UpdateState() {
        Log.d(TAG, "hdmiRxPlugged = " + String.valueOf(hdmiRxPlugged));
        if (hdmiRxPlugged) {
            home_ac_video_hdmi_nosignale.setVisibility(View.INVISIBLE);
        } else {
            home_ac_video_hdmi_nosignale.setVisibility(View.VISIBLE);
        }

        if (hdmiRxPlugged) {
            try {
                HDMIRxStatus hDMIRxStatus = this.mHDMIRX.getHDMIRxStatus();
                if (hDMIRxStatus == null || hDMIRxStatus.status != HDMIRxStatus.STATUS_READY) {
                    //this.mHandler.sendEmptyMessageDelayed(0, 200);
                    //return false;
                } else if (this.mHDMIRX.open() != 0) {

                } else {
                    //HDMIRxParameters parameters = this.mHDMIRX.getParameters();

                    Log.d(TAG, "setPreviewDisplay");
                    mHDMIRX.setPreviewDisplay(mSurfaceView.getHolder());

                    // configureTargetFormat
                    HDMIRxParameters hdmirxParam = new HDMIRxParameters();
                    //MyLog.v("hdmi setPreviewSize  mWidth = " + mWidth + "  mHeight = " + mHeight + "  mFps = " + mFps);
                    hdmirxParam.setPreviewSize(1920, 1080);
                    hdmirxParam.setPreviewFrameRate(30);
                    // set sorce format
                    mHDMIRX.setParameters(hdmirxParam);
                    // configureTargetFormat end
                    mHDMIRX.play();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }
        }
    }*/
}
