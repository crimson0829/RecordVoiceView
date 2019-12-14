package com.crimson.record

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_play.*

/**
 * @author crimson
 * @date   2019-12-14
 */
class PlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_play)

        val nodes = Utils.nodes

        val time = intent.getIntExtra("time",0)
        val height = intent.getIntExtra("height",0)

        rv_view.buildPlayNode(nodes,height,time)

        btn_play.setOnClickListener {
            if (rv_view.hasNode()) {
                rv_view.startPlay()
            }
        }

        btn_pause.setOnClickListener {
            if (rv_view.isPlaying)
                rv_view.pausePlay()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        rv_view.release()
    }
}