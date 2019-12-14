package com.crimson.record.audio

import android.media.AudioFormat
import android.media.MediaRecorder
import java.io.Serializable
import java.util.*

/**
 * @author crimson
 * @date 2019-09-11
 * 录音配置
 */
 class RecordConfig : Serializable {

    /**
     * 录音格式 默认PCM格式
     */
    var format = RecordFormat.PCM

    /**
     * 音频获取资源
     */
    var audioSource = MediaRecorder.AudioSource.MIC

    /**
     * 通道数:默认单通道
     */
    var channelConfig = AudioFormat.CHANNEL_IN_MONO

    /**
     * 位宽
     */
    private var encodingConfig = AudioFormat.ENCODING_PCM_16BIT

    /**
     * 采样率
     */
    var sampleRate = 16000

    constructor() {}

    constructor(format: RecordFormat) {
        this.format = format
    }

    /**
     * @param format         录音文件的格式
     * @param audioSource    音频获取来源
     * @param channelConfig  声道配置
     * 单声道：See [AudioFormat.CHANNEL_IN_MONO]
     * 双声道：See [AudioFormat.CHANNEL_IN_STEREO]
     * @param encodingConfig 位宽配置
     * 8Bit： See [AudioFormat.ENCODING_PCM_8BIT]
     * 16Bit: See [AudioFormat.ENCODING_PCM_16BIT],
     * @param sampleRate     采样率 hz: 8000/16000/44100
     */
    constructor(
        format: RecordFormat,
        audioSource: Int,
        channelConfig: Int,
        encodingConfig: Int,
        sampleRate: Int
    ) {
        this.format = format
        this.audioSource = audioSource
        this.channelConfig = channelConfig
        this.encodingConfig = encodingConfig
        this.sampleRate = sampleRate
    }//mp3后期转换

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    val encoding: Int
        get() {
            if (format == RecordFormat.MP3) { //mp3后期转换
                return 16
            }
            return when (encodingConfig) {
                AudioFormat.ENCODING_PCM_8BIT -> {
                    8
                }
                AudioFormat.ENCODING_PCM_16BIT -> {
                    16
                }
                else -> {
                    0
                }
            }
        }

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    val realEncoding: Int
        get() = when (encodingConfig) {
            AudioFormat.ENCODING_PCM_8BIT -> {
                8
            }
            AudioFormat.ENCODING_PCM_16BIT -> {
                16
            }
            else -> {
                0
            }
        }

    /**
     * 当前的声道数
     *
     * @return 声道数： 0：error
     */
    val channelCount: Int
        get() = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> {
                1
            }
            AudioFormat.CHANNEL_IN_STEREO -> {
                2
            }
            else -> {
                0
            }
        }


    fun format(format: RecordFormat): RecordConfig {
        this.format = format
        return this
    }

    fun channelConfig(channelConfig: Int): RecordConfig {
        this.channelConfig = channelConfig
        return this
    }

    fun getEncodingConfig(): Int {
        return if (format == RecordFormat.MP3) { //mp3后期转换
            AudioFormat.ENCODING_PCM_16BIT
        } else encodingConfig
    }

    fun encodingConfig(encodingConfig: Int): RecordConfig {
        this.encodingConfig = encodingConfig
        return this
    }

    fun sampleRate(sampleRate: Int): RecordConfig {
        this.sampleRate = sampleRate
        return this
    }

    fun audioSource(audioSource: Int): RecordConfig {
        this.audioSource = audioSource
        return this
    }

    override fun toString(): String {
        return String.format(
            Locale.getDefault(),
            "录制格式： %s,采样率：%sHz,位宽：%s bit,声道数：%s",
            format,
            sampleRate,
            encoding,
            channelCount
        )
    }

    enum class RecordFormat(val extension: String) {
        /**
         * mp3格式
         */
        MP3(".mp3"),
        /**
         * wav格式
         */
        WAV(".wav"),
        /**
         * pcm格式
         */
        PCM(".pcm");

    }


}