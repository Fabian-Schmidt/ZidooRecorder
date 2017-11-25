package net.schmidtie.presentationrecording;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.realtek.hardware.RtkHDMIRxManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class OutputWriter {
    private static final String TAG = "OutputWriter";

    private static final int ERROR = 0;
    private static final int ERRORTIME = 1000;

    private Context mContext = null;
    private RecorderClass mRecorderClass = null;

    //private FileOutputStream mFileOutputStream = null;
    private Handler mHandler = null;

    //188 bytes per TS package. 7 packages max for an MTU of 1500.
    private static final int UDP_Packet_Size = 188 * 7;
    private InetAddress mAddress = null;
    private int mPort = 0;

    private ParcelFileDescriptor mReadPipe = null;
    private ParcelFileDescriptor mUDP_WritePipe = null;
    private DisplayState mDesireState = null;
    private ParcelFileDescriptor mLocalWritePipe = null;
    private long mLocalFileSize = 0;
    private DatagramSocket udpSocket = null;
    private final int TOAST_DURATION = Toast.LENGTH_SHORT;

    //100MB
    private static final long LOCAL_COPY_MIN_FREE_DISK = 100L * 1024L * 1024L;
    //1.5GB
    private static final long LOCAL_COPY_MAX_FILE_SIZE = 1536L * 1024L * 1024L;

    public OutputWriter(Context context, RecorderClass recorderClass) {
        this.mContext = context;
        this.mRecorderClass = recorderClass;
        initHandler();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case ERROR:
                        if (((Integer) message.obj).intValue() == 1) {
                            //Toast.makeText(RecordActivity.this, "multicase_ip_error", TOAST_DURATION).show();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public ParcelFileDescriptor prepareIO(DisplayState desireState) {
        mDesireState = desireState;
        try {
            ParcelFileDescriptor[] createPipe = ParcelFileDescriptor.createPipe();
            if (createPipe == null) {
                return null;
            }
            mReadPipe = createPipe[0];
            mUDP_WritePipe = createPipe[1];
            Log.i(TAG, "mIp = " + desireState.UDP_Target_IP + "  mPort = " + desireState.UDP_Target_Port + "  Multicast = " + mAddress.isMulticastAddress());
            mAddress = InetAddress.getByName(desireState.UDP_Target_IP);
            mPort = desireState.UDP_Target_Port;
            try {
                if (desireState.HdmiVideoStream){
                    if (mAddress.isMulticastAddress() == false) {
                        udpSocket = new DatagramSocket(null);
                        udpSocket.setReuseAddress(true);
                        udpSocket.bind(null);
                    } else {
                        udpSocket = new MulticastSocket(desireState.UDP_Target_Port);
                        udpSocket.setBroadcast(true);
                        udpSocket.setReuseAddress(true);
                    }
                }
                if (desireState.HdmiVideoRecording) {
                    mLocalFileSize = 0;
                    mLocalWritePipe = GenerateNewLocalWritePipe();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.v(TAG, "e1", e);
                mUDP_WritePipe.close();
                mUDP_WritePipe = null;
                mReadPipe.close();
                mReadPipe = null;
                return null;
            }
            return this.mUDP_WritePipe;
        } catch (IOException e3) {
            Log.v(TAG, "e3", e3);
            return null;
        }
    }

    private ParcelFileDescriptor GenerateNewLocalWritePipe() {
        if (mDesireState.HdmiRecordPath != null) {
            try {
                String str = mDesireState.HdmiRecordPath.getAbsolutePath() + File.separator + GenerateLocalFileName();
                Log.d(TAG, "record path = " + str);
                //FileUtil.CreateFileWithFullAcess(recordFolder, recordFileName);
                File file = new File(str);
                file.createNewFile();
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_APPEND | ParcelFileDescriptor.MODE_WRITE_ONLY);
            } catch (IOException e) {
                Log.e(TAG, "Error during generation of new local write pipe.", e);
            }
        }
        return null;
    }

    private String GenerateLocalFileName() {
        Random r = new Random();
        int rndNumber = r.nextInt(999999);
        String recordFileName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + rndNumber + (mDesireState.HdmiFileFormat == RtkHDMIRxManager.HDMIRX_FILE_FORMAT_TS ? ".ts" : ".mp4");
        return recordFileName;
    }

    public void StartWritingOutput() {
        new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "start StartWritingOutput --- ");
                InputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(mReadPipe);
                OutputStream autoCloseLocalOutputStream = null;
                if (mLocalWritePipe != null) {
                    autoCloseLocalOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(mLocalWritePipe);
                }
                byte[] bArr = new byte[UDP_Packet_Size];
                Object obj = 1;
                int counterTillNextCheckOfFreeDiskSpace = 0;
                while (mRecorderClass.IsHdmiVideoRecording()) {
                    try {
                        if (autoCloseInputStream.available() > 0) {
                            counterTillNextCheckOfFreeDiskSpace = counterTillNextCheckOfFreeDiskSpace + 1;
                            int read = autoCloseInputStream.read(bArr);
                            if (read > 0) {
                                if (udpSocket != null) {
                                    DatagramPacket datagramPacket = new DatagramPacket(bArr, 0, read, mAddress, mPort);
                                    udpSocket.send(datagramPacket);
                                }
                                obj = 1;
                                if (autoCloseLocalOutputStream != null) {
                                    autoCloseLocalOutputStream.write(bArr, 0, read);
                                    mLocalFileSize = mLocalFileSize + read;
                                }
                            }
                        } else {
                            //All data sent for this frame.
                            if (counterTillNextCheckOfFreeDiskSpace > 30 * 30) {//every 30 seconds (@30FPS)
                                counterTillNextCheckOfFreeDiskSpace = 0;
                                if (autoCloseLocalOutputStream != null
                                        && mDesireState.HdmiRecordPath != null
                                        && mDesireState.HdmiRecordPath.getFreeSpace() < LOCAL_COPY_MIN_FREE_DISK) {
                                    //Stop recording
                                    Log.i(TAG, "Stop recording local disk is full.");
                                    try {
                                        autoCloseLocalOutputStream.flush();
                                    } catch (IOException e) {
                                        Log.e(TAG, "LocalOutputStream.flush failed.", e);
                                    }
                                    mLocalFileSize = 0;
                                    mLocalWritePipe = null;
                                    autoCloseLocalOutputStream = null;
                                    mDesireState.HdmiRecordPath = null;
                                }
                            } else if (autoCloseLocalOutputStream != null
                                    && mLocalFileSize > LOCAL_COPY_MAX_FILE_SIZE) {
                                //Max file size reached. Start new file.
                                try {
                                    autoCloseLocalOutputStream.flush();
                                } catch (IOException e) {
                                    Log.e(TAG, "LocalOutputStream.flush failed.", e);
                                }
                                mLocalFileSize = 0;
                                mLocalWritePipe = GenerateNewLocalWritePipe();
                                if (mLocalWritePipe != null) {
                                    autoCloseLocalOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(mLocalWritePipe);
                                }
                            } else {
                                //Wait for 1/6th of a frame(@30FPS) for new data.
                                Thread.sleep(5);
                            }
                        }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        if (obj != null) {
                            Log.v(TAG, "udp StartWritingOutput error", e2);
                        }
                        obj = null;
                    }
                }
                if (autoCloseLocalOutputStream != null) {
                    try {
                        autoCloseLocalOutputStream.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "LocalOutputStream.flush failed.", e);
                    }
                }
                try {
                    Log.i(TAG, "stop StartWritingOutput --- ");
                    mUDP_WritePipe.close();
                    mUDP_WritePipe = null;
                    mReadPipe.close();
                    mReadPipe = null;
                    autoCloseInputStream.close();
                } catch (Exception e22) {
                    e22.printStackTrace();
                }
                try {
                    if (udpSocket != null) {
                        udpSocket.close();
                        udpSocket = null;
                        Log.i(TAG, "stop udpSocket close --- ");
                    }
                } catch (Exception e222) {
                    e222.printStackTrace();
                }
                try {
                    if (autoCloseLocalOutputStream != null) {
                        autoCloseLocalOutputStream.close();
                        mLocalWritePipe = null;
                        Log.i(TAG, "stop LocalWritePipe close --- ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
