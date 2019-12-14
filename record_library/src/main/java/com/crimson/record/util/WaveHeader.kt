package com.crimson.record.util

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * @author crimson
 * @date 2019-09-11
 */
internal class WaveHeader {
    private val fileID = charArrayOf('R', 'I', 'F', 'F')
    var fileLength = 0
    private val wavTag = charArrayOf('W', 'A', 'V', 'E')
    private val FmtHdrID = charArrayOf('f', 'm', 't', ' ')
    var fmtHdrLeth = 0
    var formatTag: Short = 0
    var channels: Short = 0
    var samplesPerSec = 0
    var avgBytesPerSec = 0
    var blockAlign: Short = 0
    var bitsPerSample: Short = 0
    private val dataHdrID = charArrayOf('d', 'a', 't', 'a')
    var dataHdrLeth = 0
    @get:Throws(IOException::class)
    val header: ByteArray
        get() {
            val bos = ByteArrayOutputStream()
            writeChar(bos, fileID)
            writeInt(bos, fileLength)
            writeChar(bos, wavTag)
            writeChar(bos, FmtHdrID)
            writeInt(bos, fmtHdrLeth)
            writeShort(bos, formatTag.toInt())
            writeShort(bos, channels.toInt())
            writeInt(bos, samplesPerSec)
            writeInt(bos, avgBytesPerSec)
            writeShort(bos, blockAlign.toInt())
            writeShort(bos, bitsPerSample.toInt())
            writeChar(bos, dataHdrID)
            writeInt(bos, dataHdrLeth)
            bos.flush()
            val r = bos.toByteArray()
            bos.close()
            return r
        }

    @Throws(IOException::class)
    private fun writeShort(bos: ByteArrayOutputStream, s: Int) {
        val bytes = ByteArray(2)
        bytes[1] = (s shl 16 shr 24).toByte()
        bytes[0] = (s shl 24 shr 24).toByte()
        bos.write(bytes)
    }

    @Throws(IOException::class)
    private fun writeInt(bos: ByteArrayOutputStream, n: Int) {
        val buf = ByteArray(4)
        buf[3] = (n shr 24).toByte()
        buf[2] = (n shl 8 shr 24).toByte()
        buf[1] = (n shl 16 shr 24).toByte()
        buf[0] = (n shl 24 shr 24).toByte()
        bos.write(buf)
    }

    private fun writeChar(bos: ByteArrayOutputStream, id: CharArray) {
        for (i in id.indices) {
            val c = id[i]
            bos.write(c.toInt())
        }
    }
}