package net.schmidtie.util;

import android.util.Log;

/**
 * Created by Developer on 16/07/2017.
 */

public class Hdmi {
    private static final String TAG = "Hdmi";

    public static boolean isHdcp() {
        try {
            String runTimeStr = FileUtil.getRunTimeStr("/sys/class/switch/rx_hdcp/state");
            Log.d(TAG, "isHdcp state = " + runTimeStr);
            if (runTimeStr.trim().equals("1")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
