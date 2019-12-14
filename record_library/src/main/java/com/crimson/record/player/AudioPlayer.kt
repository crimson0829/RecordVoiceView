package com.crimson.record.player

import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.text.TextUtils
import java.io.IOException

/**
 * @author crimson
 * @date   2019-09-18
 * 音频播放器
 */
class AudioPlayer : BasePlayer() {

    private var mPlayingUrl: String? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mState = 0
    private var mTargetPosition = 0

    init {
        initPlayer()
    }

    override fun play(url: String?) {
        //检查是否有当前播放的数据,如果存在,比对是否相同,如果相同,调用继续播放
        //如果不相同,释放上次的mediaPlayer,继续
        //检查localPath的合法性,如果不合法,返回错误,如果合法,初始化MediaPlayer,然后做seekTo的操作
        if (!TextUtils.isEmpty(mPlayingUrl)) {
            if (mPlayingUrl == url) {
                resume()
                return
            } else {
                val filePath = mPlayingUrl!!
                initPlayer()
                notifyOnStopped(filePath)
            }
        }
        mPlayingUrl = url
        if (mPlayingUrl==null){
            return
        }
        seekTo(0)
    }

    override fun play(url: String?, pos: Int) {
        if (!TextUtils.isEmpty(mPlayingUrl)) {
            if (mPlayingUrl == url) {
                resume()
                seekTo(pos)
                return
            } else {
                val filePath = mPlayingUrl!!
                initPlayer()
                notifyOnStopped(filePath)
            }
        }
        mPlayingUrl = url
        if (mPlayingUrl==null){
            return
        }
        seekTo(pos)
    }


    override fun pause() {
        if (mState == IPlayer.RUNNING) {
            try {
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.pause()
                    notifyOnPause(mPlayingUrl)
                }
            } catch (ex: IllegalStateException) {
                handleErrorEncounted(0, 0)
            }
        } /*else if(mState == IPlayer.IDLE || mState == IPlayer.INITIALIZED){
            handleErrorEncounted(0, 0);
        }*/
    }

    override fun stop() {
        //检查状态和文件,如果不合法,释放资源,然后向外报错
        //如果合法,执行stop的操作,并且设置当前的播放状态
        if (mState == IPlayer.RUNNING || mState == IPlayer.PREPARED || mState == IPlayer.PREPARING || mState == IPlayer.INITIALIZED) {
            val filePath = mPlayingUrl!!
            initPlayer()
            notifyOnStopped(filePath)
        } /*else{
            handleErrorEncounted(0 , 0);
        }*/
    }

    override fun resume() {
        //检查状态,是否合法,然后调用start方法即可
        if (mState == IPlayer.RUNNING) {
            try {
                if (mMediaPlayer!!.isPlaying) { //do nothing
                } else {
                    mMediaPlayer!!.start()
                    checkNotifyOnPlay()
                }
            } catch (ex: IllegalStateException) {
                handleErrorEncounted(0, 0)
            }
        } /*else if(mState == IPlayer.IDLE || mState == IPlayer.INITIALIZED){
            seekTo(0);
        }*/
    }

    override fun isSupportSeekTo(): Boolean {
        return true
    }

    private fun checkNotifyOnPlay() {
        notifyOnPlay(mPlayingUrl)
    }

    override fun seekTo(targetPosition: Int) {
        //检查状态和文件,如果不合法,释放资源,然后向外报错
        //如果合法,检查当前的状态,如果是initial的状态,先标记为preparing,先做prepareAsyn的操作,
        //如果不是prepared的状态,那么直接做seekTo,在onPrepared中,设置状态为prepared的状态,然后调用started
        //Idle , need set datasource
        //Init --> Prepare
        //prepared --> run
        //run --> call start
        var targetPosition = targetPosition
        if (targetPosition < 0) {
            targetPosition = 0
        }
        mTargetPosition = targetPosition
        if (TextUtils.isEmpty(mPlayingUrl)) {
            handleErrorEncounted(0, 0)
            return
        }
        if (mState == IPlayer.IDLE) {
            try { //TODO: 是否还需要设置其他参数
                mMediaPlayer!!.setDataSource(mPlayingUrl)
            } catch (e: IOException) {
                e.printStackTrace()
                handleErrorEncounted(0, 0)
                return
            }
            mState = IPlayer.INITIALIZED
        }
        if (mState == IPlayer.INITIALIZED) {
            mState = IPlayer.PREPARING
            notifyOnPreparing(mPlayingUrl)
            mMediaPlayer!!.prepareAsync()
        } else if (mState == IPlayer.PREPARING) { //when prepared,remember seekTo
        } else if (mState == IPlayer.PREPARED) { //just start,then check seekTo
            notifyOnPrepared(mPlayingUrl)
            handlePrepared()
        } else if (mState == IPlayer.RUNNING) {
            checkSeekPlay()
        } /*else{
            Log.e(TAG, "seekTo state not right, state:"+mState);
            handleErrorEncounted(0, 0);
        }*/
    }

    private fun handlePrepared() {
        if (mState == IPlayer.PREPARED) {
            mState = IPlayer.RUNNING
            try {
                mMediaPlayer!!.start()
                checkNotifyOnPlay()
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
                handleErrorEncounted(0, 0)
                return
            }
            checkSeekPlay()
        } else {
            handleErrorEncounted(0, 0)
        }
    }

    private fun checkSeekPlay() {
        if (mState == IPlayer.RUNNING) {
            if (mTargetPosition >= 0) {
                val targetPosition = mTargetPosition
                mTargetPosition = -1
                mMediaPlayer!!.seekTo(targetPosition)
            } else {
                if (mMediaPlayer!!.isPlaying) { //do nothing
                } else {
                    mMediaPlayer!!.start()
                    checkNotifyOnPlay()
                }
            }
        } else {
            handleErrorEncounted(0, 0)
        }
    }

    override fun playPath(): String? {
        return mPlayingUrl
    }

    override fun duration(): Int {
        return if (mState == IPlayer.PREPARED || mState == IPlayer.RUNNING) {
            mMediaPlayer!!.duration
        } else -1
    }

    override fun currentPosition(): Int {
        return if (mState == IPlayer.PREPARED || mState == IPlayer.RUNNING || mState == IPlayer.INITIALIZED || mState == IPlayer.IDLE) {
            mMediaPlayer!!.currentPosition
        } else -1
    }

    override fun isPlaying(): Boolean {
        var result = false
        if (mState == IPlayer.PREPARING) {
            return false
        }
        try {
            result = mMediaPlayer!!.isPlaying
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
            handleErrorEncounted(0, 0)
        }
        return result
    }

    override fun state(): Int {
        return mState
    }

    private fun handleErrorEncounted(what: Int, extra: Int) {
        val filePath = mPlayingUrl!!
        initPlayer()
        notifyOnError(filePath, what, extra)
    }

    private fun handleComplete() {
        val filePath = mPlayingUrl!!
        initPlayer()
        notifyOnComplete(filePath)
    }

    private fun initPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.setOnPreparedListener(null)
            mMediaPlayer?.setOnErrorListener(null)
            mMediaPlayer?.setOnCompletionListener(null)
            mMediaPlayer?.setOnInfoListener(null)
            mMediaPlayer?.setOnSeekCompleteListener(null)
            //release current media player
            try {
                mMediaPlayer?.reset()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            try {
                mMediaPlayer?.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.setOnPreparedListener(mOnPreparedListener)
        mMediaPlayer?.setOnErrorListener(mOnErrorListener)
        mMediaPlayer?.setOnCompletionListener(mOnCompletionListener)
        mMediaPlayer?.setOnInfoListener(mOnInfoListener)
        mMediaPlayer?.setOnSeekCompleteListener(mOnSeekCompleteListener)
        mState = IPlayer.IDLE
        mTargetPosition = -1
        mPlayingUrl = null
    }

    private val mOnPreparedListener = OnPreparedListener {
        if (mState == IPlayer.INITIALIZED || mState == IPlayer.PREPARING) {
            mState = IPlayer.PREPARED
            notifyOnPrepared(mPlayingUrl)
            handlePrepared()
        } else {
            handleErrorEncounted(0, 0)
        }
    }

    private val mOnSeekCompleteListener = OnSeekCompleteListener { handleSeekComplete() }

    private fun handleSeekComplete() {
        if (mState == IPlayer.PREPARED) {
            handlePrepared()
        } else if (mState == IPlayer.RUNNING) {
            checkSeekPlay()
        } else { //state not right
            handleErrorEncounted(0, 0)
        }
    }


    private val mOnErrorListener =
        OnErrorListener { mp, what, extra ->
            handleErrorEncounted(what, extra)
            true
        }

    private val mOnCompletionListener = OnCompletionListener { handleComplete() }

    private val mOnInfoListener =
        OnInfoListener { mp, what, extra -> false }
}