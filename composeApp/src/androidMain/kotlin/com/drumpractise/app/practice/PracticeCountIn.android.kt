package com.drumpractise.app.practice

import android.media.AudioTrack
import com.drumpractise.app.R
import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.data.drumApplicationContext
import com.drumpractise.app.metronome.RawResourceMonoPcmDecoder
import com.drumpractise.app.metronome.STREAM_PCM_SAMPLE_RATE
import com.drumpractise.app.metronome.createStreamAudioTrack48kMono
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val SILENCE_CHUNK_SHORTS = 4096

/** 写入 [data] 中从 [offset] 开始的 [length] 个样本（[length] 可为截断后的长度）。 */
private fun writeShortsFully(
    track: AudioTrack,
    data: ShortArray,
    offset: Int = 0,
    length: Int = data.size - offset,
) {
    val end = (offset + length).coerceAtMost(data.size)
    var pos = offset
    while (pos < end) {
        val w = track.write(data, pos, end - pos)
        if (w == AudioTrack.ERROR_INVALID_OPERATION ||
            w == AudioTrack.ERROR_BAD_VALUE ||
            w == AudioTrack.ERROR_DEAD_OBJECT
        ) {
            break
        }
        if (w <= 0) break
        pos += w
    }
}

private suspend fun writeSilenceSamples(
    track: AudioTrack,
    totalShorts: Int,
) {
    var remaining = totalShorts
    val chunk = ShortArray(SILENCE_CHUNK_SHORTS)
    while (remaining > 0) {
        coroutineContext.ensureActive()
        val n = remaining.coerceAtMost(chunk.size)
        var o = 0
        while (o < n) {
            val w = track.write(chunk, o, n - o)
            if (w <= 0) return
            o += w
        }
        remaining -= n
    }
}

actual suspend fun playPracticeCountIn(
    bpm: Int,
    onBeat: ((Int) -> Unit)?,
) {
    val ctx = drumApplicationContext()
    val sr = STREAM_PCM_SAMPLE_RATE
    val rawId = R.raw.drum_hi_hat_closed
    val bpmClamped = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
    val msPerQuarter = 30000.0 / bpmClamped
    val samplesPerQuarter = (sr * msPerQuarter / 1000.0).roundToInt().coerceAtLeast(1)

    val track = createStreamAudioTrack48kMono() ?: return

    try {
        withContext(Dispatchers.IO) {
            val clip = RawResourceMonoPcmDecoder.decodeMonoS16Resampled(ctx, rawId, sr)
            track.play()
            for (beat in 0..7) {
                coroutineContext.ensureActive()
                withContext(Dispatchers.Main) {
                    if (beat % 2 == 0) onBeat?.invoke(beat / 2 + 1)
                }
                val clipSamples = clip.size
                val writeLen = clipSamples.coerceAtMost(samplesPerQuarter)
                writeShortsFully(track, clip, offset = 0, length = writeLen)
                if (writeLen < samplesPerQuarter) {
                    writeSilenceSamples(track, samplesPerQuarter - writeLen)
                }
            }
        }
    } finally {
        try {
            track.pause()
            track.flush()
        } catch (_: Exception) {
        }
        track.release()
    }
}
