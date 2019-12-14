package com.crimson.record.player

/**
 * @author crimson
 * @date 2019-09-18
 * 播放器回调
 */
 interface IPlayerListener {
    /**
     * 正在准备播放当前的文件
     * @param filePath
     */
    fun onPreparing(filePath: String?)

    /**
     * 当前文件的播放准备完毕
     * @param filePath
     */
    fun onPrepared(filePath: String?)

    /**
     * 暂停播放当前文件
     * @param filePath
     */
    fun onPause(filePath: String?)

    /**
     * 正在播放当前文件
     * @param filePath
     */
    fun onPlay(filePath: String?)

    /**
     * 播放当前文件出错
     * @param filePath
     * @param what
     * @param extra
     */
    fun onError(filePath: String?, what: Int, extra: Int)

    /**
     * 当前文件播放停止(停止,可能是由于播放其他文件停止)
     * @param filePath
     */
    fun onStopped(filePath: String?)

    /**
     * 当前文件播放完成,正常播放完成
     * @param filePath
     */
    fun onComplete(filePath: String?)
}