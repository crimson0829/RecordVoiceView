package com.crimson.record.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Process
import com.crimson.record.util.calculateVolume

/**
 *
 * 描述：录音实现类
 *
 *
 * 使用举例：<br></br>
 * AudioRecorder recorder = new AudioRecorder.Builder()<br></br>
 * .rate(8000)<br></br>
 * .audioEncoding(AudioFormat.ENCODING_PCM_16BIT)<br></br>
 * .audioSource(MediaRecorder.AudioSource.MIC)<br></br>
 * .channelConfiguration(AudioFormat.CHANNEL_IN_MONO)<br></br>
 * .build();<br></br>
 * recorder.startRecord()<br></br>
 * recorder.stopRecord()<br></br>
 *
 */
class AudioRecorder private constructor(builder: Builder) : BaseAudio() {

    private var mAudioRecord: AudioRecord? = null
    //录音配置
    var recordConfig: RecordConfig? = null
    private var minBufferSize = 0
    private var bufferReadResult: Int? = 0
    private var readBufferSzie = 1024
    private var mConnectListener: IConnectListener? = null
    private var mAudioDecoder: IAudioDecoder? = null
    private var mPause = false
    //是否需要分贝回调结果
    private var needDbResult = false


    init {
        config(builder)
    }

    /**
     * 创建一个新的builder参数
     *
     * @param builder
     */
    fun newBulder(builder: Builder) {
        config(builder)
    }

    private fun config(builder: Builder) {
        recordConfig = builder.recordConfig
        readBufferSzie = builder.readBufferSize
        mConnectListener = builder.connectListener
    }

    public override fun doStart() {}

    override fun doRestart() {
    }

    private fun initAudioRecorder() {
        try {
            minBufferSize = AudioRecord.getMinBufferSize(
                recordConfig!!.sampleRate,
                recordConfig!!.channelConfig,
                recordConfig!!.getEncodingConfig()
            )
            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
                if (mConnectListener != null) {
                    mConnectListener?.onFailure(
                        IllegalStateException("initialization err"),
                        minBufferSize
                    )
                }
                return
            }
            mAudioRecord = AudioRecord(
                recordConfig!!.audioSource,
                recordConfig!!.sampleRate,
                recordConfig!!.channelConfig,
                recordConfig!!.getEncodingConfig(),
                minBufferSize * 10
            )
            var nState = mAudioRecord?.state
            if (nState != AudioRecord.STATE_INITIALIZED) {
                mAudioRecord?.release()
                mAudioRecord = null
                if (mConnectListener != null) {
                    mConnectListener?.onFailure(
                        IllegalStateException("initialization err"),
                        nState
                    )
                }
                return
            }
            if (readBufferSzie <= 0 || readBufferSzie > minBufferSize) {
                readBufferSzie = minBufferSize
            }
            nState = mAudioRecord?.recordingState
            if (nState != AudioRecord.STATE_INITIALIZED) {
                mAudioRecord?.release()
                mAudioRecord = null
                if (mConnectListener != null) {
                    mConnectListener?.onFailure(
                        IllegalStateException("initialization err"),
                        nState
                    )
                }
                return
            }
            mAudioRecord?.startRecording()
            //阻塞直到开始转态
            while (mAudioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) { // Wait until we can startRecord recording...
            }
            if (mConnectListener != null) {
                mConnectListener?.onSuccess()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (mConnectListener != null) {
                mConnectListener?.onFailure(e, -1)
            }
        }
    }

    public override fun doRun() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            initAudioRecorder()
            if (mAudioDecoder != null) mAudioDecoder?.startDecoder(recordConfig?.sampleRate)
            val audio_data = ByteArray(readBufferSzie)
            while (isRunning()!!) { //如果是暂停状态不录音
                if (mPause) {
                    if (mAudioDecoder != null) mAudioDecoder?.recorderState(mPause)
                    Thread.sleep(100)
                    continue
                }
                bufferReadResult = mAudioRecord?.read(audio_data, 0, audio_data.size)
                //print(Arrays.toString(audio_data));
                if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                    continue
                }
                if (bufferReadResult!! > 0) {
                    if (mAudioDecoder != null) {
                        mAudioDecoder?.decoderData(audio_data, bufferReadResult)
                        //回调分贝
                        if (needDbResult) {
                            mAudioDecoder?.onDbResult(calculateVolume(audio_data))
                        }
                    }
                }
            }
            if (mAudioDecoder != null) mAudioDecoder?.finishDecoder()
            try {
                Thread.sleep(250)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (mAudioRecord != null) {
                try {
                    mAudioRecord?.stop()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                mAudioRecord?.release()
                mAudioRecord = null
            }
        }

    }

    /**
     * 暂定
     */
    override fun doPause() {
        mPause = true
    }

    /**
     * 继续
     */
    fun resume() {
        mPause = false
    }

    fun audioDecoder(audioDecoder: IAudioDecoder?) {
        mAudioDecoder = audioDecoder
    }

    fun needDbResult(needDbResult: Boolean) {
        this.needDbResult = needDbResult
    }


    class Builder internal constructor() {
        var recordConfig: RecordConfig
        var readBufferSize = 0
        var connectListener: IConnectListener? = null

        fun connectListener(connectListener: IConnectListener?): Builder {
            this.connectListener = connectListener
            return this
        }

        /**
         * readBufferSzie<=0则用minBufferSize，否则用minBufferSize
         *
         * @param val
         * @return
         */
        fun readBufferSize(size: Int): Builder {
            readBufferSize = size
            return this
        }

        fun recordConfig(config: RecordConfig): Builder {
            recordConfig = config
            return this
        }

        fun build(): AudioRecorder {
            return AudioRecorder(this)
        }

        init {
            recordConfig = RecordConfig()
                .channelConfig(AudioFormat.CHANNEL_IN_MONO)
                .sampleRate(16000)
        }
    }


    public override fun doStop() {}

    override fun doRelease() {

        stopRecord()
        mAudioRecord?.release()
        mAudioRecord = null
        mConnectListener = null
        mAudioDecoder = null
    }



    /**
     *
     * 描述：解码接口
     * 1.所有回调方法都在线程中执行<br></br>
     */
    interface IAudioDecoder {
        /**
         * 开始解码
         */
        fun startDecoder(sampleRate: Int?)

        /**
         * 回调信号数据
         *
         * @param audio_data       读取的信号数据
         * @param bufferReadResult 读取的实际数据长度
         */
        @Throws(Exception::class)
        fun decoderData(audio_data: ByteArray?, bufferReadResult: Int?)

        /**
         * 分贝的回调
         *
         * @param db
         */
        fun onDbResult(db: Int?)

        /**
         * 结束解码
         */
        fun finishDecoder()

        /**
         * 录音转态
         *
         * @param isPause //是否是暂停
         */
        fun recorderState(isPause: Boolean?)
    }

    /**
     *
     * 描述：录音连接转态
     */
    interface IConnectListener {
        /**
         * 连接成功
         */
        fun onSuccess()

        /**
         * 连接失败
         *
         * @param code
         */
        fun onFailure(e: Exception?, code: Int?)
    }


}