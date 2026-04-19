package com.drumpractise.app.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build

/** 与节拍器 / 预备拍共用：48kHz 单声道 PCM 流式 [AudioTrack]。 */
internal const val STREAM_PCM_SAMPLE_RATE: Int = 48_000

internal fun createStreamAudioTrack48kMono(): AudioTrack? {
    val minBytes =
        AudioTrack.getMinBufferSize(
            STREAM_PCM_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
    if (minBytes <= 0) return null
    val bufferBytes = minBytes
    val attrs =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    val audioFormat =
        AudioFormat
            .Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(STREAM_PCM_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
    val trackBuilder =
        AudioTrack
            .Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
    }
    val track = trackBuilder.build()
    if (track.state != AudioTrack.STATE_INITIALIZED) {
        track.release()
        return null
    }
    return track
}
