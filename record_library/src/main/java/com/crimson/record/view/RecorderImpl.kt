package com.crimson.record.view

import android.content.Context
import android.util.Log
import com.crimson.record.audio.AudioRecorder
import com.crimson.record.bus.RxBus
import com.crimson.record.bus.RxCode
import com.crimson.record.bus.RxDisposable
import com.crimson.record.util.RxRecordTime
import com.crimson.record.util.checkMicAvailable
import com.crimson.record.util.isNetworkConnected
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * @author crimson
 * @date   2019-9-20
 * 录音实现
 * 振幅、节点、时间、网络状态实现
 * <p>
 * 根据录音模式来断句 ：如果是有网模式，在默认4秒内没有说话，就断句
 * 如果是本地模式，在默认15秒内没说话，就断句
 * 具体的录音模式参考 {@link IRecorder.RecordMode}
 * 录音状态参考{@link IRecorder.RecordStatus}
 * 切割方式参考 ONLINE模式下如果5秒未接受识别数据，就切割节点
 * LOCAL模式下每15秒切割一次节点
 * 有网状态下的模式需自己实现
 */
internal class RecorderImpl(context: Context) : IRecorder {

    //有网录音
    private var onlineRecorder: IRecorder.IOnlineRecorder? = null

    //默认的本地录音实现
    private var localRecorder: AudioRecorder? = null

    //录音模式
    private var recordMode: IRecorder.RecordMode = if (isNetworkConnected(context)) {
        IRecorder.RecordMode.ONLINE
    } else {
        IRecorder.RecordMode.LOCAL
    }

    //初始状态
    private var recordStatus: IRecorder.RecordStatus = IRecorder.RecordStatus.IDLE

    companion object {
        //切割计数
        @Volatile
        var clipTime = 0

    }


    //切割倒计时
    private var timeS: Disposable? = null
    //网络状态
    private var netS: Disposable? = null
    //录音记时
    private var recordTimeS: Disposable? = null

    //有网状态切割间隔
    private var onlineClipTime = 5

    //无网状态切割间隔
    private var localClipTime = 15


    init {
        registerRxBus()
    }


    /**
     * 注册rx bus
     */
   private fun registerRxBus() {

        //网络状态改变
        netS = RxBus.get()?.toObservable(RxCode.NET_STATE_CHANGE_CODE, Boolean::class.java)
            ?.subscribe {

                if (recordStatus !== IRecorder.RecordStatus.RECORDING) {
                    return@subscribe
                }

                //发送一个有网或者无网的bus
                RxBus.get()
                    ?.post(if (it) RxCode.NET_STATE_ONLINE_CODE else RxCode.NET_STATE_LOCAL_CODE, 0)

                restartRecord()
            }


        RxDisposable.add(netS)


    }

    /**
     * 重新开始录音，
     * 由于其中一种的麦克风会被占用
     * 所以先释放所有的模式。再开启其中一种模拟
     * 两种模式切换的过程中，录音机的获取会比较慢释放，要检查录音机是否可用
     */
    override fun restartRecord() {

        //先释放所有模式
        if (localRecorder != null) {
            localRecorder?.release()
        }

        if (onlineRecorder != null) {
            onlineRecorder?.release()
        }

        checkAudio()

        //重新开启一种模式
        if (recordMode === IRecorder.RecordMode.ONLINE) {
            onlineRecorder?.restartRecord()
        } else {
            startLocalDefault()
        }
        startNodeClip()
        recordStatus = IRecorder.RecordStatus.RECORDING
    }


    /**
     * 开启默认的本地录音模式
     */
    private fun startLocalDefault() {

        if (localRecorder == null) {
            localRecorder = AudioRecorder.Builder().build()

            val localDecoder = object : AudioRecorder.IAudioDecoder {
                override fun startDecoder(sampleRate: Int?) {
                }

                override fun decoderData(audio_data: ByteArray?, bufferReadResult: Int?) {
                    //pcm数据发送，做统一处理
                    RxBus.get()?.post(RxCode.POST_PCM_DATA, audio_data)
                }

                override fun onDbResult(db: Int?) {
                    //分贝db数据发送，做统一处理
                    RxBus.get()?.post(RxCode.POST_VOICE_DB, db)
                }

                override fun finishDecoder() {
                }

                override fun recorderState(isPause: Boolean?) {
                }

            }

            localRecorder?.audioDecoder(localDecoder)

            if (localRecorder?.isRunning()!!) {
                localRecorder?.resume()
            } else {
                localRecorder?.startRecord()
            }

        }

    }

    /**
     * 阻塞直到获取麦克风释放
     */
    @Synchronized
    private fun checkAudio() {
        if (!checkMicAvailable()) {
            checkAudio()
        }
    }


    /**
     * 设置有网模式录音实现
     */
    fun onlineRecorder(recorder: IRecorder.IOnlineRecorder): RecorderImpl {
        this.onlineRecorder = recorder
        return this
    }

    /**
     * 设置有网状态下切割时间
     */
    fun onlineClipTime(time: Int): RecorderImpl {
        this.onlineClipTime = time
        return this
    }

    /**
     * 设置本地状态下切割时间
     */
    fun localClipTime(time: Int): RecorderImpl {
        this.localClipTime = time
        return this
    }

    /**
     * 录音模式设置
     */
    fun recordMode(mode:IRecorder.RecordMode): RecorderImpl{
        this.recordMode=mode
        return this
    }


    override fun startRecord() {
        when (recordMode) {
            IRecorder.RecordMode.ONLINE -> {
                onlineRecorder?.startRecord()
            }
            IRecorder.RecordMode.LOCAL -> {
                localRecorder?.startRecord()
            }
        }
        recordStatus = IRecorder.RecordStatus.RECORDING

        recordTimeS = RxRecordTime.startRecordTime()

        startNodeClip()

    }

    override fun pauseRecord() {

        when (recordMode) {
            IRecorder.RecordMode.ONLINE -> {
                onlineRecorder?.pauseRecord()
            }
            IRecorder.RecordMode.LOCAL -> {
                localRecorder?.pauseRecord()
            }
        }

        stopNodeClip()

        recordStatus = IRecorder.RecordStatus.PAUSED

        RxRecordTime.pauseOrStopRecordTime(recordTimeS)
    }

    override fun stopRecord() {
        when (recordMode) {
            IRecorder.RecordMode.ONLINE -> {
                onlineRecorder?.stopRecord()
            }
            IRecorder.RecordMode.LOCAL -> {
                localRecorder?.stopRecord()
            }

        }

        recordStatus = IRecorder.RecordStatus.STOP

        RxRecordTime.pauseOrStopRecordTime(recordTimeS)
    }


    override fun release() {

        onlineRecorder?.release()
        localRecorder?.release()

        stopNodeClip()

        RxDisposable.remove(netS)

        recordStatus = IRecorder.RecordStatus.FINISHED

        RxRecordTime.releaseRecordTime(recordTimeS)

    }

    override fun recordMode(): IRecorder.RecordMode = recordMode

    override fun recordStatus(): IRecorder.RecordStatus = recordStatus

    /**
     *  开启切割倒计时
     *
     */
    private fun startNodeClip() {

        timeS = Flowable.interval(1, 1, TimeUnit.SECONDS)
            .subscribe({
                synchronized(this) {
                    clipTime += 1
                }

                Log.w("startNodeClip", clipTime.toString())

                if (recordMode == IRecorder.RecordMode.ONLINE) {
                    if (clipTime >= onlineClipTime) {
                        Log.w("startNodeClip", "有网切割")
                        //切割
                        RxBus.get()?.post(RxCode.RECORD_CLIP_CODE, 0)
                        clipTime = 0
                    }
                } else {
                    if (clipTime >= localClipTime) {
                        RxBus.get()?.post(RxCode.RECORD_CLIP_CODE, 0)
                        clipTime = 0
                    }
                }
            }, {
                stopNodeClip()
                startNodeClip()
            })

        RxDisposable.add(timeS)


    }

    /**
     * 结束倒计时
     */
   private fun stopNodeClip() {

        clipTime = 0

        RxDisposable.remove(timeS)

    }
}