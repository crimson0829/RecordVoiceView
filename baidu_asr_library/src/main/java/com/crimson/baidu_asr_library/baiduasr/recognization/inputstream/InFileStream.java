package com.crimson.baidu_asr_library.baiduasr.recognization.inputstream;

import android.app.Activity;

import com.crimson.baidu_asr_library.baiduasr.util.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;



public class InFileStream {

    private static Activity context;

    private static final String TAG = "InFileStream";

    public static void setContext(Activity context) {
        InFileStream.context = context;
    }

    private static String filename;

    private static InputStream is;

    public static void reset() {
        filename = null;
        is = null;
    }

    public static void setFileName(String filename) {
        InFileStream.filename = filename;
    }

    public static String getFilename() {
        return filename;
    }

    public static void setInputStream(InputStream is2) {
        is = is2;
    }

    public static InputStream create16kStream() {
        if (is != null) {
            return new FileAudioInputStream(is);
        } else if (filename != null) {
            try {
                return new FileAudioInputStream(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            return new FileAudioInputStream(createFileStream());
        }
        return null;
    }

    private static InputStream createFileStream() {
        InputStream res = null;
        try {
            InputStream is = context.getAssets().open("outfile.pcm");
            Logger.info(TAG, "create input stream ok " + is.available());
            res = new FileAudioInputStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}