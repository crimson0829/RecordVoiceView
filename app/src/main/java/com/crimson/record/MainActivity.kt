package com.crimson.record

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.crimson.baidu_asr_library.impl.BDOnlineRecordImpl
import com.crimson.record.data.RecordNodeData
import com.crimson.record.notification.RecordNotification
import com.crimson.record.view.IRecorder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val notify by lazy {
        RecordNotification(this,this::class.java)
//            .smallIcon()
//            .largeIcon()
//            .recordingTitle()
//            .pauseTitle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        ActivityCompat.requestPermissions(this,
             arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_PHONE_STATE), 0)

        rv_view.apply {
            //设置有网状态下录音实现
            setOnlineRecord(BDOnlineRecordImpl(context))
            //设置有网切割时间间隔
            setOnlineClipTime(5)
            //设置本地切割时间间隔
            setLocalClipTime(15)
            //设置录音模式
            setRecordMode(IRecorder.RecordMode.ONLINE)
            //监听器设置
            setCallBack {

                statusChangedCallBack {

                    println("status -> ${it.toString()}")

                }


            }
        }


        btn_rec.setOnClickListener {
            rv_view.startRecord()
            notify.show()
        }

        btn_pause.setOnClickListener {
            rv_view.pauseRecord()
            notify.pause()
        }

        btn_complete.setOnClickListener {

            val nodes = rv_view.completeRecord()
            if (nodes != null) {
                Utils.nodes.addAll(nodes)
            }
            val intent = Intent(this, PlayActivity::class.java).apply {
                putExtra("time",rv_view.totalTime())
                putExtra("height",rv_view.totalHeight())
            }

            startActivity(intent)

        }


    }

    override fun onDestroy() {
        super.onDestroy()
        rv_view.release()
        notify.release()
    }

}

object Utils{

   val nodes = arrayListOf<RecordNodeData>()

}
