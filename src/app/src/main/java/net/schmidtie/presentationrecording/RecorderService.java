package net.schmidtie.presentationrecording;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Developer on 29/07/2017.
 */

public class RecorderService extends Service {
    private static final String TAG = "RecorderService";
    private IBinder mBinder = new LocalBinder();
    private RecorderClass mRecorderClass = null;

    public class LocalBinder extends Binder {
        public RecorderService getService() {
            return RecorderService.this;
        }
    }

    private void init() {
        this.mRecorderClass = new RecorderClass(this);
        //this.mAlarmTool = new AlarmTool(this, this.mRecorderInterface);
    }

    public RecorderClass getRecorderInterface() {
    return this.mRecorderClass;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return this.mBinder;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate");
        init();
        super.onCreate();
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        this.mRecorderClass.onDestroy();
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            //long longExtra = intent.getLongExtra("recordTime", -1);
            //MyLog.m18v(TAG, "RecorderService recordTime = " + longExtra);
            //if (longExtra > 0 && longExtra > System.currentTimeMillis()) {
                //this.mAlarmTool.startRecorder(longExtra);
            //}
            /*int intExtra = intent.getIntExtra("pip", -1);
            MyLog.m18v(TAG, "RecorderService pip = " + intExtra);
            if (intExtra == 0) {
                if (this.mRecorderInterface.isPip()) {
                    this.mRecorderInterface.stopPip();
                }
            } else if (intExtra == 1) {
                if (!this.mRecorderInterface.isPip()) {
                    this.mRecorderInterface.startPip();
                }
            } else if (intExtra == 2) {
                if (this.mRecorderInterface.isPip()) {
                    this.mRecorderInterface.stopPip();
                } else {
                    this.mRecorderInterface.startPip();
                }
            }
            intExtra = intent.getIntExtra("record", -1);
            MyLog.m18v(TAG, "RecorderService record = " + intExtra);
            if (intExtra == 0) {
                if (this.mRecorderInterface.isRecord()) {
                    this.mRecorderInterface.stopRecord();
                }
            } else if (intExtra == 1) {
                if (!this.mRecorderInterface.isRecord()) {
                    this.mRecorderInterface.startRecord();
                }
            } else if (intExtra == 2) {
                if (this.mRecorderInterface.isRecord()) {
                    this.mRecorderInterface.stopRecord();
                } else {
                    this.mRecorderInterface.startRecord();
                }
            }
            intExtra = intent.getIntExtra("udp", -1);
            MyLog.m18v(TAG, "RecorderService udp = " + intExtra);
            if (intExtra == 0) {
                if (this.mRecorderInterface.isUdp()) {
                    this.mRecorderInterface.stopUdp();
                }
            } else if (intExtra == 1) {
                if (!this.mRecorderInterface.isUdp()) {
                    this.mRecorderInterface.startUdp();
                }
            } else if (intExtra == 2) {
                if (this.mRecorderInterface.isUdp()) {
                    this.mRecorderInterface.stopUdp();
                } else {
                    this.mRecorderInterface.startUdp();
                }
            }*/
        }
        return super.onStartCommand(intent, i, i2);
    }

    public void release() {

    }
}
