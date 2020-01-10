package com.crimson.record.util

import com.crimson.record.bus.RxBus
import com.crimson.record.bus.RxCode
import com.crimson.record.data.RecordTime
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * @author crimson
 * @date 2019-09-14
 * rx 计时器
 */
internal class RxRecordTime {

    companion object {

        var recordTime = 0

        /**
         * 开启计时器
         */
        fun startRecordTime(): Disposable =
            Flowable.interval(0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { aLong ->
                    if (aLong != 0L) {
                        recordTime += 1
                    }
                    val sec: Long = recordTime % 60.toLong()
                    val min: Long = recordTime / 60.toLong()
                    RxBus.get()?.post(RxCode.RECORD_TIME_CODE, RecordTime(min, sec))
                }


        /**
         * 暂停或停止计时器
         */
        fun pauseOrStopRecordTime(sub: Disposable?) {
            if (sub != null && !sub.isDisposed) {
                sub.dispose()
            }
        }

        /**
         * 释放计时器
         */
        fun releaseRecordTime(sub: Disposable?) {
            pauseOrStopRecordTime(sub)
            recordTime = 0
        }

    }


}



