package com.crimson.record.bus

/**
 * @author crimson
 * @date   2019-09-20
 * rx bus code
 */
object RxCode {

    /**
     * 网络状态发生改变
     */
    const val NET_STATE_CHANGE_CODE = -0xff01
    const val NET_STATE_ONLINE_CODE = -0xff02
    const val NET_STATE_LOCAL_CODE = -0xff03
    /**
     * 发送pcm数据
     *
     */
    const val POST_PCM_DATA = -0xff04
    /**
     * 发送分贝
     */
    const val POST_VOICE_DB = -0xff05

    /**
     * 有网状态下识别
     */
    const val ONLINE_RECORD_STATUS_CODE = -0xff06
    //识别部分转化
    const val ONLINE_RECORD_PARTIAL = -0xff07
    //识别结束
    const val ONLINE_RECORD_FINISHED = -0xff08
    //识别错误
    const val ONLINE_RECORD_ERROR = -0xff09

    /**
     * 录音切割
     */
    const val RECORD_CLIP_CODE = -0xff10

    /**
     * 计时器
     */
    const val RECORD_TIME_CODE = -0xff11


}