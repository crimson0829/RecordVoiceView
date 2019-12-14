package com.crimson.record.data

import android.os.Parcelable
import android.text.StaticLayout
import android.widget.EditText
import kotlinx.android.parcel.Parcelize

/**
 * @author crimson
 * @date 2019-09-06
 * 录音节点对象
 * 包含文件对象[RecordFile]
 */
@Parcelize
data class RecordNodeData(
    //节点索引标记
    var index: Int,
    //偏移量
    var distance: Int,
    //录音开始时间
    var time: String,
    //语音内容
    var content: String
) : Parcelable {
    //文件时长
    var duration: String? = null

    //绘制翻译内容的布局
    var contentLayout: StaticLayout? = null

    //file对象
    var recordFile: RecordFile? = null

    //编辑edittext,在编辑内容状态下添加
    var editText: EditText? = null

}

/**
 * @author crimson
 * @date 2019-09-05
 * 录音时间对象
 */
internal data class RecordTime(var min: Long, var sec: Long)