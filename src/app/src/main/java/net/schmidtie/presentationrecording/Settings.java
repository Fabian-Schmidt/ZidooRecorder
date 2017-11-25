package net.schmidtie.presentationrecording;

import com.google.gson.Gson;

public class Settings {
    public boolean Autostart = false;

    public boolean RecordLocal = true;
    public boolean RecordAllowInternal = false;

    public boolean StreamUDP = true;
    public String StreamUDP_IP = "239.0.0.1";
    public int StreamUDP_Port = 5000;

    public int QualityVideoBitrate = 15000000;
    public boolean QualityVideoReduceFramerate = true;
    public boolean QualityVideoLimitResolution = true;
    public int QualityAudioSamples = 44100;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
