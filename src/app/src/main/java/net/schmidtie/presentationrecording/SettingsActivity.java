package net.schmidtie.presentationrecording;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {
    private static final String TAG = "SettingsActivity";

    private static final String KEY_AUTOSTART = "autostart";
    private static final String KEY_RECORD_LOCAL = "record_local";
    private static final String KEY_RECORD_ALLOW_INTERNAL = "record_allow_internal";
    private static final String KEY_UDP = "stream_udp";
    private static final String KEY_UDP_IP = "stream_udp_ip";
    private static final String KEY_UDP_PORT = "stream_udp_port";
    private static final String KEY_QUALITY_VIDEO_BITRATE = "quality_video_bitrate";
    private static final String KEY_QUALITY_VIDEO_REDUCE_FRAMERATE = "quality_video_reduce_framerate";
    private static final String KEY_QUALITY_VIDEO_LIMIT_RESOLUTION = "quality_video_limit_resolution";
    private static final String KEY_QUALITY_AUDIO_BITRATE = "quality_audio_samples";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static Settings ReadSettings(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Settings ret = new Settings();

        ret.Autostart = sharedPref.getBoolean(SettingsActivity.KEY_AUTOSTART, false);

        ret.RecordLocal = sharedPref.getBoolean(SettingsActivity.KEY_RECORD_LOCAL, true);
        ret.RecordAllowInternal = sharedPref.getBoolean(SettingsActivity.KEY_RECORD_ALLOW_INTERNAL, false);

        ret.StreamUDP = sharedPref.getBoolean(SettingsActivity.KEY_UDP, true);
        ret.StreamUDP_IP = sharedPref.getString(SettingsActivity.KEY_UDP_IP, "239.0.0.1");
        String StreamUDP_Port = sharedPref.getString(SettingsActivity.KEY_UDP_PORT, "5000");
        try {
            ret.StreamUDP_Port = Integer.parseInt(StreamUDP_Port);
        } catch (NumberFormatException ex) {
            ret.StreamUDP_Port = 5000;
        }

        String QualityVideoBitrate = sharedPref.getString(SettingsActivity.KEY_QUALITY_VIDEO_BITRATE, "15000000");
        try {
            ret.QualityVideoBitrate = Integer.parseInt(QualityVideoBitrate);
        } catch (NumberFormatException ex) {
            ret.QualityVideoBitrate = 15000000;
        }
        ret.QualityVideoReduceFramerate = sharedPref.getBoolean(SettingsActivity.KEY_QUALITY_VIDEO_REDUCE_FRAMERATE, true);
        ret.QualityVideoLimitResolution = sharedPref.getBoolean(SettingsActivity.KEY_QUALITY_VIDEO_LIMIT_RESOLUTION, true);
        String QualityAudioSamples = sharedPref.getString(SettingsActivity.KEY_QUALITY_AUDIO_BITRATE, "48000");
        try {
            ret.QualityAudioSamples = Integer.parseInt(QualityAudioSamples);
        } catch (NumberFormatException ex) {
            ret.QualityAudioSamples = 48000;
        }

        return ret;
    }
}
