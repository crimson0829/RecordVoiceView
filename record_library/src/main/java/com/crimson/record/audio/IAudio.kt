package com.crimson.record.audio

import com.crimson.record.view.IRecorder

/**
 *
 * 音频接口
 *
 */
interface IAudio: IRecorder.ILocalRecorder {

    /**
     * 是否在运行中
     */
    fun isRunning(): Boolean?
}