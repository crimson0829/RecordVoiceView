package com.crimson.record.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import com.crimson.record.bus.RxBus.Companion.get
import com.crimson.record.bus.RxCode
import com.crimson.record.data.RecordTime
import io.reactivex.disposables.Disposable

/**
 * @author crimson
 * @date 2019-9-18
 * 录音状态通知栏
 */
class RecordNotification(
    val context: Context,
    val targetClass: Class<out Activity?>
) {


    private var mNotificationManager: NotificationManager? = null
    private var mId = 0
    private var timeS: Disposable? = null
    private var mBuilder: Notification.Builder? = null
    private var recordingTile = "正在录音.."
    private var pauseTitle = "录音已暂停"

    init {
        init()
        initRxbus()
    }

    private fun init() {
        mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                targetClass.name.hashCode().toString(),
                "Record", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.enableLights(false) //是否在桌面icon右上角展示小红点
            channel.setShowBadge(false) //是否在久按桌面图标时显示此渠道的通知
            if (mNotificationManager != null) {
                mNotificationManager!!.createNotificationChannel(channel)
            }
        }
        mBuilder = Notification.Builder(context)
        val intent = Intent(context, targetClass)
        mId = intent.hashCode()
        val contentIntent =
            PendingIntent.getActivity(context, mId, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        mBuilder!!.setContentIntent(contentIntent)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.sym_def_app_icon
                )
            )
            .setContentTitle(recordingTile)
            .setContentText("00:00")
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder!!.setChannelId(targetClass.name.hashCode().toString())
        }
    }

    private fun initRxbus() {
        timeS = get()?.toObservable(RxCode.RECORD_TIME_CODE, RecordTime::class.java)
            ?.subscribe({ (min, sec) ->
                if (mNotificationManager != null && mBuilder != null) {
                    val mins = if (min < 10) "0$min" else min.toString()
                    val secs = if (sec < 10) "0$sec" else sec.toString()
                    mBuilder!!.setContentText("$mins:$secs")
                    val build = mBuilder!!.build()
                    mNotificationManager!!.notify(mId, build) //展示
                }
            }, {})
    }

    /**
     * 设置正在录音文字
     */
    fun recordingTitle(title: String): RecordNotification {
        recordingTile = title
        return this
    }

    /**
     * 设置暂停文字
     */
    fun pauseTitle(title: String): RecordNotification {
        pauseTitle = title
        return this
    }


    /**
     * 设置小图标
     */
    fun smallIcon(@DrawableRes res: Int): RecordNotification {
        mBuilder?.setSmallIcon(res)
        return this
    }

    /**
     * 设置大图标
     */
    fun largeIcon(@DrawableRes res: Int): RecordNotification {
        mBuilder?.setLargeIcon(
            BitmapFactory.decodeResource(
                context.resources,
                res
            )
        )

        return this
    }


    /**
     * 展示
     */
    fun show() {
        if (mNotificationManager != null && mBuilder != null) {
            mBuilder!!.setContentTitle(recordingTile)
            val build = mBuilder!!.build()
            //            build.flags = Notification.FLAG_ONGOING_EVENT; // 设置常驻 Flag
            mNotificationManager!!.notify(mId, build)
        }
    }

    /**
     * 暂定
     */
    fun pause() {
        if (mNotificationManager != null && mBuilder != null) {
            mBuilder!!.setContentTitle(pauseTitle)
            val build = mBuilder!!.build()
            mNotificationManager!!.notify(mId, build)
        }
    }

    /**
     * 释放
     */
    fun release() {
        if (timeS != null) {
            timeS!!.dispose()
            timeS = null
        }
        if (mNotificationManager != null) {
            mNotificationManager!!.cancel(mId)
        }
    }


}