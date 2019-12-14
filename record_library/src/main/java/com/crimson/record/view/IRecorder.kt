package com.crimson.record.view

/**
 * @author crimson
 * @date 2019-09-10
 * 录音接口
 */
interface IRecorder {
    /**
     * 开始录音
     */
    fun startRecord()

    /**
     * 重新开始录音
     */
    fun restartRecord()

    /**
     * 暂停录音
     */
    fun pauseRecord()

    /**
     * 停止录音
     */
    fun stopRecord()

    /**
     * 释放
     */
    fun release()


    /**
     * 录音模式
     */
    fun recordMode():RecordMode

    /**
     * 录音状态
     */
    fun recordStatus():RecordStatus

    /**
     * 录音模式
     * 有网模式跟本地模式
     * 如果在有网状态下，ONLINE
     * 如果在断网情况下，LOCAL
     */
    enum class RecordMode {
        ONLINE, LOCAL
    }

    /**
     * 录音的状态 ：空闲，录音中 ，暂停，停止，结束
     */
    enum class RecordStatus {
        IDLE, RECORDING, PAUSED, STOP,FINISHED
    }

    /**
     *有网模式下接口
     */
    interface IOnlineRecorder :IRecorder

    /**
     * 本地模式下接口
     */
    interface ILocalRecorder:IRecorder

}
