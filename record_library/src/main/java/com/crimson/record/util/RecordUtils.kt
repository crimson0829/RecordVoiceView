@file:Suppress("DEPRECATION")

package com.crimson.record.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import com.crimson.record.audio.RecordConfig
import java.io.*
import kotlin.math.abs
import kotlin.math.log10

/**
 * @author crimson
 * @date   2019-11-30
 * 工具函数
 */

/**
 * 判断是否有网
 */
internal  fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager = context
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

/**
 * 检测麦克风是否可用
 *
 */
internal fun checkMicAvailable(): Boolean {
    var available = true
    var recorder: AudioRecord? = AudioRecord(
        MediaRecorder.AudioSource.MIC, 8000,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_DEFAULT, 8000
    )
    try {
        if (recorder!!.recordingState != AudioRecord.RECORDSTATE_STOPPED) {
            available = false
        }
        recorder.startRecording()
        if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            recorder.stop()
            available = false
        }
        recorder.stop()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        recorder!!.release()
        recorder = null
    }
    return available
}


/**
 * 计算分贝大小
 * @param buffer
 *
 */
internal fun calculateVolume(buffer: ByteArray): Int {
    var sumVolume = 0.0
    var avgVolume = 0.0
    var volume = 0.0
    var i = 0
    while (i < buffer.size) {
        val v1: Int = buffer[i].toInt() and 0xFF
        val v2: Int = buffer[i + 1].toInt() and 0xFF
        var temp = v1 + (v2 shl 8) // 小端
        if (temp >= 0x8000) {
            temp = 0xffff - temp
        }
        sumVolume += abs(temp).toDouble()
        i += 2
    }
    avgVolume = sumVolume / buffer.size / 2
    volume = log10(1 + avgVolume) * 10
    return volume.toInt()
}

/**
 * pcm文件转wav
 *
 */
internal fun convertFilePcmToWav(
    pcmFile: File,
    wavFile: File,
    config: RecordConfig
) {
    var fis: FileInputStream? = null
    var fos: FileOutputStream? = null
    try {
        fis = FileInputStream(pcmFile)
        fos = FileOutputStream(wavFile)
        val buf = ByteArray(1024 * 1000)
        var size = fis.read(buf)
        var PCMSize = 0
        while (size != -1) {
            PCMSize += size
            size = fis.read(buf)
        }
        fis.close()
        val header = WaveHeader()
        header.fileLength = PCMSize + (44 - 8)
        header.fmtHdrLeth = 16
        header.bitsPerSample = config.encoding.toShort()
        header.channels = config.channelCount.toShort()
        header.formatTag = config.audioSource.toShort()
        header.samplesPerSec = config.sampleRate
        header.blockAlign = (header.channels * header.bitsPerSample / 8).toShort()
        header.avgBytesPerSec = header.blockAlign * header.samplesPerSec
        header.dataHdrLeth = PCMSize
        val h: ByteArray = header.header
        //         h.length == 44;
        //write header
        fos.write(h, 0, h.size)
        //write data stream
        fis = FileInputStream(pcmFile)
        size = fis.read(buf)
        while (size != -1) {
            fos.write(buf, 0, size)
            size = fis.read(buf)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            fis?.close()
            fos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


/**
 * WAV转PCM文件
 * @param wavFilePath wav文件路径
 * @param pcmFilePath pcm要保存的文件路径及文件名
 *
 */
internal  fun convertFileWavToPcm(
    wavFilePath: String,
    pcmFilePath: String
): String {
    val fis: FileInputStream
    val fos: FileOutputStream
    try {
        fis = FileInputStream(wavFilePath)
        fos = FileOutputStream(pcmFilePath)
        val wavByte = inputStreamToByte(fis)
        val pcmByte = wavByte.copyOfRange(44, wavByte.size)
        fos.write(pcmByte)
        closeIO(fis)
        closeIO(fos)
    } catch (e: java.lang.Exception) {
        println(e.message)
    }
    return pcmFilePath
}

/**
 * 输入流转byte二进制数据
 * @param fis
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
private fun inputStreamToByte(fis: FileInputStream): ByteArray {
    val byteStream = ByteArrayOutputStream()
    val size = fis.channel.size()
    val buffer: ByteArray?
    if (size <= Int.MAX_VALUE) {
        buffer = ByteArray(size.toInt())
    } else {
        buffer = ByteArray(8)
        for (ix in 0..7) {
            val offset = 64 - (ix + 1) * 8
            buffer[ix] = (size shr offset and 0xff).toByte()
        }
    }
    var len: Int
    while (fis.read(buffer).also { len = it } != -1) {
        byteStream.write(buffer, 0, len)
    }
    val data = byteStream.toByteArray()
    closeIO(byteStream)
    return data
}

private fun closeIO(closeable: Closeable?) {
    try {
        closeable?.close()
    } catch (var2: IOException) {
        var2.printStackTrace()
    }
}