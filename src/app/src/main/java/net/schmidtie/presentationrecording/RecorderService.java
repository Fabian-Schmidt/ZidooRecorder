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
        return super.onStartCommand(intent, i, i2);
    }

    public void release() {

    }
}
