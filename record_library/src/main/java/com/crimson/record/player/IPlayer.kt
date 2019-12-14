package com.crimson.record.player

/**
 * @author crimson
 * @date 2019-09-18
 * 音频播放器接口
 */
 interface IPlayer {
    //other state not stated above will just release mediaplayer,so needn't care
    /**
     * 播放某个音频
     * @param url 在线url或者本地的路径
     */
    fun play(url: String?)

    /**
     * 播放某个音频,直接从对应的位置开始播放
     * @param url
     * @param pos
     */
    fun play(url: String?, pos: Int)

    /**
     * 暂停播放,如果没有播放的音频文件,会忽略
     */
    fun pause()

    /**
     * 停止播放,会清空当前播放的信息,如果当前没有播放的文件,会忽略
     */
    fun stop()

    /**
     * 继续播放,如果没有播放的音频文件,会忽略
     */
    fun resume()

    /**
     * 是否支持seekTo
     * @return
     */
    fun isSupportSeekTo(): Boolean

    /**
     * seekTo操作,如果当前没有播放的音频文件
     * @param targetPosition
     */
    fun seekTo(targetPosition: Int)

    fun playPath(): String?
    fun duration(): Int
    fun currentPosition(): Int
    fun isPlaying(): Boolean
    fun state(): Int
    fun addPlayListener(listener: IPlayerListener?)
    fun removePlayListener(listener: IPlayerListener?)

    companion object {
        /**
         * mediaplayer is idle
         */
        const val IDLE = 0
        /**
         * mediaplayer is initialized
         */
        const val INITIALIZED = 1
        /**
         * mediaplayer is preparing
         */
        const val PREPARING = 2
        /**
         * mediaplayer is prepared
         */
        const val PREPARED = 3
        /**
         * mediaplayer is started or paused
         */
        const val RUNNING = 4
    }
}