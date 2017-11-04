package net.schmidtie.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by Developer on 16/07/2017.
 */

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String getRunTimeStr(String str) {
        String readLine;
        IOException e;
        try {
            InputStream inputStream = Runtime.getRuntime().exec("cat " + str).getInputStream();
            Reader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            readLine = bufferedReader.readLine();
            if (readLine == null) {
                readLine = null;
            }
            try {
                bufferedReader.close();
                inputStreamReader.close();
                inputStream.close();
            } catch (IOException e2) {
                e = e2;
                e.printStackTrace();
                return readLine;
            }
        } catch (IOException e3) {
            IOException iOException = e3;
            readLine = null;
            e = iOException;
            e.printStackTrace();
            return readLine;
        }
        return readLine;
    }

    public static String getSystemProperties(String str) {
        try {
            return new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("getprop " + str).getInputStream())).readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean setSystemProperties(String str, String str2) {
        try {
            Runtime.getRuntime().exec("setprop " + str + " " + str2);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String GetStorageFolder() {
        String removableStoragePath = null;
        java.io.File fileList[] = new java.io.File("/storage/").listFiles();
        for (java.io.File file : fileList) {
            Log.d(TAG, "GetStorageFolder = " + file.getAbsolutePath());
            if (!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath())
                    && file.isDirectory()
                    && file.canRead()) {
                removableStoragePath = file.getAbsolutePath();
            }
        }
        return removableStoragePath;
    }

    public static void CreateFileWithFullAcess(String foldername, String filename) {
        try {
            Process p=Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes("mkdir -p " + foldername + "\n");
            dos.writeBytes("touch " + foldername + File.separator + filename + "\n");
            dos.writeBytes("chmod 666 " + foldername + File.separator + filename + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            dos.close();
            p.waitFor();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void ChangeFileToFullAcess(String filename) {
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "777", filename});
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
