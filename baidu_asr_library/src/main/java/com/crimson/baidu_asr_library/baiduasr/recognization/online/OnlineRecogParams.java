package com.crimson.baidu_asr_library.baiduasr.recognization.online;

import android.content.Context;

import com.baidu.speech.asr.SpeechConstant;
import com.crimson.baidu_asr_library.baiduasr.recognization.CommonRecogParams;

import java.util.Arrays;



public class OnlineRecogParams extends CommonRecogParams {


    private static final String TAG = "OnlineRecogParams";


    public OnlineRecogParams(Context context) {
        super(context);

        stringParams.addAll(Arrays.asList(
            "_language", // 用于生成PID参数
            "_model" // 用于生成PID参数
        ));

        intParams.addAll(Arrays.asList(SpeechConstant.PROP));

        boolParams.addAll(Arrays.asList(SpeechConstant.DISABLE_PUNCTUATION));

    }


}
