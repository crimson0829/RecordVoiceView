package com.crimson.baidu_asr_library.impl

import android.content.Context
import android.os.Handler
import android.os.Message
import com.baidu.speech.asr.SpeechConstant
import com.crimson.baidu_asr_library.baiduasr.control.BaiduAsrRecognizerManager
import com.crimson.baidu_asr_library.baiduasr.recognization.IStatus
import com.crimson.baidu_asr_library.baiduasr.recognization.MessageStatusRecogListener
import com.crimson.record.bus.RxBus
import com.crimson.record.bus.RxBusMessage
import com.crimson.record.bus.RxCode
import com.crimson.record.view.IRecorder

/**
 * @author crimson
 * @date   2019-9-10
 * 百度联网状态下录音实现
 */
class BDOnlineRecordImpl(val context: Context) : IRecorder.IOnlineRecorder {

    private var recordStatus: IRecorder.RecordStatus = IRecorder.RecordStatus.IDLE

    private var mRecogListener: MessageStatusRecogListener? = null
    private var mAsrManager: BaiduAsrRecognizerManager? = null

    // 录音类型 0：长语音 1：短语音
    private var type = 0

    private var mHandler =
        Handler(Handler.Callback { msg: Message ->
            when (msg.arg1) {
                IStatus.STATUS_PARTIAL -> {
                    val psg = msg.obj as String
                    RxBus.get()?.post(
                        RxCode.ONLINE_RECORD_STATUS_CODE,
                        RxBusMessage(RxCode.ONLINE_RECORD_PARTIAL, psg)
                    )
                }
                IStatus.STATUS_FINISHED -> {
                    val msg = msg.obj as String
                    RxBus.get()?.post(
                        RxCode.ONLINE_RECORD_STATUS_CODE,
                        RxBusMessage(RxCode.ONLINE_RECORD_FINISHED, msg)
                    )
                }
                IStatus.STATUS_LONG_FINISHED -> {
                }
                IStatus.STATUS_ERROR -> {
                    val errorCode = msg.obj as String
                    RxBus.get()?.post(
                        RxCode.ONLINE_RECORD_STATUS_CODE,
                        RxBusMessage(RxCode.ONLINE_RECORD_ERROR, errorCode)
                    )
                }
            }
            false
        })

    private fun initAsr() {
        mRecogListener = MessageStatusRecogListener(mHandler)
        mRecogListener?.setOnShowPcmDataListener(object :
            MessageStatusRecogListener.OnShowPcmDataListener {
            override fun onShowPcmData(data: ByteArray?, offset: Int, length: Int) {
                /**
                 * 这里把pcm数据写入到文件当中,发送到[RecordManager]统一处理
                 *
                 */
                RxBus.get()?.post(RxCode.POST_PCM_DATA, data)
            }

            override fun onShowAsrVolume(volumePercent: Int, volume: Int) { //这里获取音量大小
                RxBus.get()?.post(RxCode.POST_VOICE_DB, volume / 10)
            }
        })

        mAsrManager = BaiduAsrRecognizerManager(context.applicationContext, mRecogListener)
    }

    init {
        initAsr()
    }

    /**
     * 设置录音类型
     */
    fun setType(type: Int): BDOnlineRecordImpl {
        this.type = type
        return this
    }


    /**
     * 设置长语音参数
     *
     * @return
     */
    private fun fetchLongRecParams(): Map<String, Any>? {

        val map = hashMapOf<String, Any>()
        //音频数据回调，回调的不准，
        map[SpeechConstant.ACCEPT_AUDIO_DATA] = true
        //音量回调
        map[SpeechConstant.ACCEPT_AUDIO_VOLUME] = true
        //长语音模式
        map[SpeechConstant.VAD] = SpeechConstant.VAD_DNN
        //标点
        map[SpeechConstant.DISABLE_PUNCTUATION] = false
        //采样率
        map[SpeechConstant.SAMPLE_RATE] = 16000
        //pid
        map[SpeechConstant.PID] = 1537
        //长语音识别超时
        map[SpeechConstant.VAD_ENDPOINT_TIMEOUT] = 0

        return map
    }

    /**
     * 设置默认在线语音参数
     *
     * @return
     */
    private fun fetchCommonParams(): Map<String, Any>? {
        val map = hashMapOf<String, Any>()
        map[SpeechConstant.ACCEPT_AUDIO_DATA] = true
        map[SpeechConstant.ACCEPT_AUDIO_VOLUME] = true
        map[SpeechConstant.VAD] = SpeechConstant.VAD_TOUCH
        map[SpeechConstant.PID] = 1537
        return map
    }


    /**
     * 长语音识别
     */
    private fun startLong() {
        mAsrManager?.start(fetchLongRecParams())

    }


    /**
     * 60秒语音在线识别
     */
    private fun startCommon() {
        mAsrManager?.start(fetchCommonParams())
    }

    /**
     * 停止识别
     */
    private fun stop() {

        mAsrManager?.cancel()
    }


    override fun startRecord() {
        if (0 == type) {
            startLong()
        } else {
            startCommon()
        }
    }

    override fun restartRecord() {
        initAsr()
        startRecord()
    }

    override fun pauseRecord() {
        stop()
    }

    override fun stopRecord() {
        stop()
    }

    override fun release() {
        mAsrManager?.release()
        mAsrManager = null
        mHandler.removeCallbacksAndMessages(null)
        mRecogListener = null
    }

    override fun recordMode(): IRecorder.RecordMode {
        return IRecorder.RecordMode.ONLINE

    }

    override fun recordStatus(): IRecorder.RecordStatus {
        return recordStatus
    }
}