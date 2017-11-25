package net.schmidtie.presentationrecording;

import com.realtek.hardware.RtkHDMIRxManager;

import java.io.File;

public class DisplayState {
    public boolean HdmiVideo = false;
    public boolean HdmiAudio = false;

    public boolean HdmiVideoRecording = false;
    public int HdmiRecordTime = 0;

    public int HdmiFileFormat = RtkHDMIRxManager.HDMIRX_FILE_FORMAT_TS;

    public boolean HdmiRecordToDeviceAllowed = false;
    //public String HdmiRecordPath = null;
    public File HdmiRecordPath = null;
    public String HdmiRecordFile = null;

    public int Fps = 0;
    public int Width = 0;
    public int Height = 0;
    public int VideoBitrate = 0;
    public boolean VideoReduceFramerate = true;
    public int AudioSamples = 0;

    public boolean HdmiVideoStream = true;
    public String UDP_Target_IP = "239.0.0.1";
    public int UDP_Target_Port = 5000;
}
