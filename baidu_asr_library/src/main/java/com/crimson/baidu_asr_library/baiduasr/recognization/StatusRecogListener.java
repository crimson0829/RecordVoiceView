package com.crimson.baidu_asr_library.baiduasr.recognization;


import com.crimson.baidu_asr_library.baiduasr.util.Logger;

public class StatusRecogListener implements IRecogListener, IStatus {

    private static final String TAG = "StatusRecogListener";

    /**
     * 识别的引擎当前的状态
     */
    protected int status = STATUS_NONE;

    @Override
    public void onAsrReady() {
        status = STATUS_READY;
    }

    @Override
    public void onAsrBegin() {
        status = STATUS_SPEAKING;
    }

    @Override
    public void onAsrEnd() {
        status = STATUS_RECOGNITION;
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        status = STATUS_PARTIAL;

    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        status = STATUS_FINISHED;
    }

    @Override
    public void onAsrFinish(RecogResult recogResult) {
        status = STATUS_FINISHED;
    }


    @Override
    public void onAsrFinishError(int errorCode, int subErrorCode, String errorMessage, String descMessage,
                                 RecogResult recogResult) {
        Logger.error(TAG,"StatusRecog  errorCode = " + errorCode + "  subErrorCode = " + subErrorCode + " errorMessage = " + errorMessage + " descMessage = " + descMessage);
        status = STATUS_ERROR;
    }

    /**
     * 长语音识别结束
     */
    @Override
    public void onAsrLongFinish() {
        status = STATUS_LONG_FINISHED;
    }

    @Override
    public void onAsrVolume(int volumePercent, int volume) {
//        LogUtils.w( "音量百分比" + volumePercent + " ; 音量" + volume);
    }

    @Override
    public void onAsrAudio(byte[] data, int offset, int length) {
//        if (offset != 0 || data.length != length) {
//            byte[] actualData = new byte[length];
//            System.arraycopy(data, 0, actualData, 0, length);
//            data = actualData;
//        }

//        LogUtils.w( "音频数据回调, length:" + data.length);
    }

    @Override
    public void onAsrExit() {
        status = STATUS_NONE;
    }

    @Override
    public void onAsrOnlineNluResult(String nluResult) {
        status = STATUS_FINISHED;
    }

    @Override
    public void onOfflineLoaded() {

    }

    @Override
    public void onOfflineUnLoaded() {

    }



}