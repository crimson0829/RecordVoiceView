package com.crimson.record.data

import android.util.Log
import com.crimson.record.audio.RecordConfig
import com.crimson.record.util.convertFilePcmToWav
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

/**
 * @author crimson
 * @date 2019-09-10
 * 录音文件对象
 */
class RecordFile(
    var file: File,
    var recordConfig: RecordConfig
) {


    init {
        val exist = createOrExistsFile(file)
        if (!exist) {
            Log.e(
                "RecordFile",
                " not exist file , you can't record voice!! please create the record file !"
            )
        }
    }


    //开始时间戳
    private val startTime = System.currentTimeMillis()
    //结束时间戳
    private var endTime: Long = 0
    //判断文件是否转化为了wav
    private var isConvertWav = false
    private val sink by lazy {
        file.sink().buffer()
    }
    //判段流是否关闭
    var isClosed = false


    /**
     * 写入文件
     *
     * @param data
     */
    @Synchronized
    fun writeData(data: ByteArray?) {

        if (isClosed) {
            return
        }
        try {

            sink.write(data!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    /**
     * 关闭流，并转化为wav文件
     *
     */
    @Synchronized
    fun pcmToWav() {

        if (isClosed) {
            return
        }
        try {
            isClosed = true
            sink.flush()
            sink.close()
        } catch (e: Exception) { // 打印异常信息
            e.printStackTrace()
        } finally {
            endTime = System.currentTimeMillis()
            makeWav()
        }


    }

    /**
     * 获取录音时长 单位秒
     *
     * @return
     */
    fun fileDuration(): String {
        if (endTime == 0L) {
            return "0"
        }
        val duration = (endTime - startTime) / 1000
        return duration.toString()
    }

    /**
     * 判断文件是否转化
     * 如果一直未转化完成，阻塞线程 就不让点完成
     *
     * @return
     */
    @Synchronized
    fun checkFileConvert() {

        if (!isConvertWav) {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            checkFileConvert()
        }

    }


    /**
     * 添加Wav头文件
     */
    @Synchronized
    private fun makeWav() {

        if (!isFile(file) || file.length() == 0L) {
            isConvertWav = true
            return
        }

        val absolutePath = file.absolutePath.replace(".pcm", ".wav")


        val wavFile = File(absolutePath)
        createOrExistsFile(wavFile)
        try {
            convertFilePcmToWav(file, wavFile, recordConfig)
            deleteFile(file)
            file = wavFile

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isConvertWav = true
        }
    }


}


/**
 * Return whether it is a file.
 *
 * @param file The file.
 * @return `true`: yes<br></br>`false`: no
 */
internal fun isFile(file: File?): Boolean {
    return file != null && file.exists() && file.isFile
}

/**
 * Create a file if it doesn't exist, otherwise do nothing.
 *
 * @param file The file.
 * @return `true`: exists or creates successfully<br></br>`false`: otherwise
 */
internal fun createOrExistsFile(file: File?): Boolean {
    if (file == null) return false
    if (file.exists()) return file.isFile
    return if (!createOrExistsDir(file.parentFile)) false else try {
        file.createNewFile()
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

/**
 * Create a directory if it doesn't exist, otherwise do nothing.
 *
 * @param file The file.
 * @return `true`: exists or creates successfully<br></br>`false`: otherwise
 */
internal fun createOrExistsDir(file: File?): Boolean {
    return file != null && if (file.exists()) file.isDirectory else file.mkdirs()
}

/**
 * Delete the file.
 *
 * @param file The file.
 * @return `true`: success<br></br>`false`: fail
 */
internal fun deleteFile(file: File?): Boolean {
    return file != null && (!file.exists() || file.isFile && file.delete())
}
