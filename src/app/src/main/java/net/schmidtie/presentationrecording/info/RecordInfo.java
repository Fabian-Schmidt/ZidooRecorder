package net.schmidtie.presentationrecording.info;

import android.content.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import org.json.JSONException;
import org.json.JSONObject;

public class RecordInfo implements Cloneable {
    public static final int AUTO = 0;
    public static final String BRODCAST_IP = "255.255.255.255";
    public static final int HINT_RESERVATION_TIME = 30000;
    public static final int MIN_RESERVATION_TIME = 60000;
    public static final String MULTICAST_IP = "239.0.0.1";
    public static final String ONE_IP = "192.168.11.215";
    public static final int OUTPUTFORMAT_MP4 = 1;
    public static final int OUTPUTFORMAT_TS = 0;
    public static final String PORT = "7878";
    public static final int RESOLUTION_1080P = 2;
    public static final int RESOLUTION_4K = 1;
    public static final int RESOLUTION_720P = 3;
    public static final int UDP_USE_BRODCAST = 1;
    public static final int UDP_USE_MULTICAST = 0;
    public static final int UDP_USE_ONE = 2;
    public boolean isExfat = false;
    public int mBitRate = 0;
    public String mBroadcasePort = PORT;
    public int mChannelCount = 0;
    public String mCurrentRecordFileName = "";
    public String mCurrentRecordFilePath = "";
    public int mCurrentRecordFrameRate = 0;
    public int mFrameRate = 0;
    public File mIdentifier = null;
    public String mIp = MULTICAST_IP;
    public String mMulticastIp = MULTICAST_IP;
    public String mMulticastPort = PORT;
    public String mOnePort = PORT;
    public String mOnetIp = ONE_IP;
    public int mOutputFormat = 0;
    public String mPath = "/mnt/sdcard/hdmi";
    public String mPort = PORT;
    public int mRecoerFileIndex = 0;
    public long mReservationTime = 0;
    public int mResolution = 0;
    public int mSampleRate = 1;
    public long mTime = 0;
    public int mType = 0;
    public int mUDPType = 0;
    public long mVideoLength = 0;

    public static String getResolutionText(ResolutionInfo resolutionInfo) {
        return (resolutionInfo.mWidth == 3840 && resolutionInfo.mHeight == 2160) ? "4K" : (resolutionInfo.mWidth == 1920 && resolutionInfo.mHeight == 1080) ? "1080P" : (resolutionInfo.mWidth == 1280 && resolutionInfo.mHeight == 720) ? "720P" : resolutionInfo.mWidth + "x" + resolutionInfo.mHeight;
    }

    /*public static String getResolutionTextKey(Context context, ResolutionInfo resolutionInfo) {
        return resolutionInfo.isAuto ? context.getString(C0248R.string.auto) : getResolutionText(resolutionInfo);
    }*/

    public Object clone() {
        Object obj;
        CloneNotSupportedException e;
        try {
            obj = (RecordInfo) super.clone();
            /*try {
                //FileIdentifier fileIdentifier = new FileIdentifier(this.mIdentifier.getType(), this.mIdentifier.getUri(), this.mIdentifier.getUuid());
                //fileIdentifier.setPassword(this.mIdentifier.getPassword());
                //fileIdentifier.setExtra(this.mIdentifier.getExtra());
                //fileIdentifier.setUser(this.mIdentifier.getUser());
                //obj.mIdentifier = fileIdentifier;
            } catch (CloneNotSupportedException e2) {
                e = e2;
                e.printStackTrace();
                return obj;
            }*/
        } catch (CloneNotSupportedException e3) {
            CloneNotSupportedException cloneNotSupportedException = e3;
            obj = null;
            e = cloneNotSupportedException;
            e.printStackTrace();
            return obj;
        }
        return obj;
    }

    /*
    public String[] getBitRate(Context context) {
        return context.getResources().getStringArray(C0248R.array.bitrate_key);
    }

    public int[] getBitRateValues(Context context) {
        return context.getResources().getIntArray(C0248R.array.bitrate_values);
    }

    public String[] getChannelCount(Context context) {
        return context.getResources().getStringArray(C0248R.array.channelcount_key);
    }

    public String[] getFrameRate(Context context) {
        return context.getResources().getStringArray(C0248R.array.framerate_key);
    }

    public String[] getOutputFormat(Context context) {
        return context.getResources().getStringArray(C0248R.array.outputformat_key);
    }

    public String[] getOutputMode(Context context) {
        return context.getResources().getStringArray(C0248R.array.mode_key);
    }

    public String[] getSampleRate(Context context) {
        return context.getResources().getStringArray(C0248R.array.samplerate_key);
    }

    public String getTime() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Long(this.mReservationTime));
    }

    public String[] getUdpSystle(Context context) {
        return context.getResources().getStringArray(C0248R.array.udp_key);
    }*/

    public String getVideoLength() {
        if (this.mVideoLength <= 0) {
            return "Auto";
        }
        int i = (int) (this.mVideoLength / 3600);
        int i2 = (int) ((this.mVideoLength % 3600) / 60);
        int i3 = (int) (this.mVideoLength % 60);
        Object stringBuilder = new StringBuilder(String.valueOf(i)).toString();
        if (i < 10) {
            stringBuilder = "0" + i;
        }
        String stringBuilder2 = new StringBuilder(String.valueOf(i2)).toString();
        if (i2 < 10) {
            stringBuilder2 = "0" + i2;
        }
        String stringBuilder3 = new StringBuilder(String.valueOf(i3)).toString();
        if (i3 < 10) {
            stringBuilder3 = "0" + i3;
        }
        return new StringBuilder(String.valueOf(stringBuilder)).append(":").append(stringBuilder2).append(":").append(stringBuilder3).toString();
    }

    public String getVideoLength(long j) {
        if (j <= 0) {
            return "00:00:00";
        }
        int i = (int) (j / 3600);
        int i2 = (int) ((j % 3600) / 60);
        int i3 = (int) (j % 60);
        Object stringBuilder = new StringBuilder(String.valueOf(i)).toString();
        if (i < 10) {
            stringBuilder = "0" + i;
        }
        String stringBuilder2 = new StringBuilder(String.valueOf(i2)).toString();
        if (i2 < 10) {
            stringBuilder2 = "0" + i2;
        }
        String stringBuilder3 = new StringBuilder(String.valueOf(i3)).toString();
        if (i3 < 10) {
            stringBuilder3 = "0" + i3;
        }
        return new StringBuilder(String.valueOf(stringBuilder)).append(":").append(stringBuilder2).append(":").append(stringBuilder3).toString();
    }
/*
    public void saveFileIdentifier(Context context, FileIdentifier fileIdentifier) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("type", fileIdentifier.getType());
            jSONObject.put("uri", fileIdentifier.getUri());
            jSONObject.put("uuid", fileIdentifier.getUuid());
            if (fileIdentifier.getUser() != null) {
                jSONObject.put("user", fileIdentifier.getUser());
            }
            if (fileIdentifier.getPassword() != null) {
                jSONObject.put("password", fileIdentifier.getPassword());
            }
            if (fileIdentifier.getExtra() != null) {
                jSONObject.put("extra", fileIdentifier.getExtra());
            }
            context.getSharedPreferences("config", 0).edit().putString(BrowseConstant.EXTRA_IDENTIFIER, jSONObject.toString()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.mIdentifier = fileIdentifier;
    }*/
}
