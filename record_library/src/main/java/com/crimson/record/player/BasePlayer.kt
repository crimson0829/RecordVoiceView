package com.crimson.record.player

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author crimson
 * @date 2019-09-18
 * base 播放器
 */
abstract class BasePlayer : IPlayer {

    private val mIRecordListenerSet: MutableSet<IPlayerListener> =
        CopyOnWriteArraySet()

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    override fun addPlayListener(listener: IPlayerListener?) {
        if (listener != null) {
            mIRecordListenerSet.add(listener)
        }
    }

    override fun removePlayListener(listener: IPlayerListener?) {
        if (listener != null) {
            mIRecordListenerSet.remove(listener)
        }
    }

    protected fun notifyOnPrepared(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onPrepared(filePath)
            }
        }
    }

    protected fun notifyOnPreparing(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onPreparing(filePath)
            }
        }
    }

    protected fun notifyOnPlay(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onPlay(filePath)
            }
        }
    }

    protected fun notifyOnPause(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onPause(filePath)
            }
        }
    }

    protected fun notifyOnError(filePath: String?, what: Int, extra: Int) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onError(filePath, what, extra)
            }
        }
    }

    protected fun notifyOnStopped(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onStopped(filePath)
            }
        }
    }

    protected fun notifyOnComplete(filePath: String?) {
        mHandler.post {
            for (next in mIRecordListenerSet) {
                next.onComplete(filePath)
            }
        }
    }

}