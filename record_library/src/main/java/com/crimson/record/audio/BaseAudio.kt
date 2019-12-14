package com.crimson.record.audio

import com.crimson.record.view.IRecorder
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 *
 * 描述：音频线程基类
 * 1.开启线程<br></br>
 * 2.停止线程<br></br>
 * 3.是否在运行中<br></br>
 */
abstract class BaseAudio : Runnable, IAudio {
    private var isRun = false
    private val mLock: ReadWriteLock =
        ReentrantReadWriteLock()
    private var mThread: Thread? = null

    private var recordStatus:IRecorder.RecordStatus=IRecorder.RecordStatus.IDLE


    override fun startRecord() {
        mLock.readLock().lock()
        try {
            if (isRun) {
                return
            }
            doStart()
            mThread = Thread(this)
            mThread?.start()
        } finally {
            mLock.readLock().unlock()
        }

        recordStatus=IRecorder.RecordStatus.RECORDING
    }

    override fun restartRecord() {
        doRestart()
        recordStatus=IRecorder.RecordStatus.RECORDING
    }


    override fun pauseRecord() {
        doPause()
        recordStatus=IRecorder.RecordStatus.PAUSED
    }

    override fun release() {
        doRelease()
        recordStatus=IRecorder.RecordStatus.FINISHED
    }

    override fun stopRecord() {
        mLock.readLock().lock()
        try {
            if (!isRun) {
                return
            }
            isRun = false
            doStop()
            //mThread.sleep(50);
            //mThread.interrupt();
            mThread?.join(150)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            mLock.readLock().unlock()
        }
        recordStatus=IRecorder.RecordStatus.STOP
    }


    override fun isRunning(): Boolean? {
        return isRun
    }


    override fun run() {
        isRun = true
        doRun()
    }

    override fun recordMode(): IRecorder.RecordMode =IRecorder.RecordMode.LOCAL

    override fun recordStatus(): IRecorder.RecordStatus = recordStatus


    protected abstract fun doStart()
    protected abstract fun doRestart()
    protected abstract fun doRun()
    protected abstract fun doPause()
    protected abstract fun doStop()
    protected abstract fun doRelease()

}