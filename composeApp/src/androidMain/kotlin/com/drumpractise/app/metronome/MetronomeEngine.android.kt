package com.drumpractise.app.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.content.Context
import com.drumpractise.app.R
import com.drumpractise.app.data.drumApplicationContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.roundToInt

actual class MetronomeEngine actual constructor() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var executor = Executors.newSingleThreadExecutor()
    private var loopFuture: Future<*>? = null
    @Volatile private var running = false
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null
    private var storedConfig: MetronomeRunConfig? = null

    /** Decoded mono S16 PCM at [METRONOME_PCM_SAMPLE_RATE], keyed by `R.raw` id. */
    private val pcmByRawId = ConcurrentHashMap<Int, ShortArray>()

    @Volatile private var pcmOutTrack: AudioTrack? = null

    actual fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit) {
        stopLoop()
        storedOnBeat = onBeat
        storedConfig = config
        running = true
        val bpm = config.bpm.coerceIn(10, 300)
        val noteDivisor = config.noteDivisor.coerceIn(1, 4)
        val preset = config.preset
        val period = metronomeBeatPeriod(noteDivisor)
        val intervalSamples = METRONOME_PCM_SAMPLE_RATE * 60.0 / bpm / noteDivisor
        loopFuture =
            executor.submit {
                val ctx = drumApplicationContext()
                ensurePcmDecodedForPreset(ctx, preset)
                val minBytes =
                    AudioTrack.getMinBufferSize(
                        METRONOME_PCM_SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                if (minBytes <= 0) return@submit
                // 使用系统允许的最小缓冲以降低端到端延迟；过小会提高 underrun 风险（听感为偶发断音/咯噔）。
                val bufferBytes = minBytes
                val attrBuilder =
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                val attrs = attrBuilder.build()
                val audioFormat =
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(METRONOME_PCM_SAMPLE_RATE)
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
                    return@submit
                }
                pcmOutTrack = track
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val capacityShorts = bufferBytes / PCM_BYTES_PER_MONO_FRAME
                // 较小 chunk → 更频繁 write，便于在浅缓冲下尽快补数据；过小会增加系统调用开销。
                val chunkSamples =
                    (capacityShorts / 8).coerceIn(MIN_CHUNK_SAMPLES, MAX_CHUNK_SAMPLES)
                val silence = ShortArray(chunkSamples)
                var primedShorts = 0
                // 预填「短」于半缓冲，减少首声前固定延迟；单位与 write(short[]) 返回值一致（short 个数）。
                val primeUpper = (capacityShorts / 2).coerceAtLeast(1)
                val primeTargetShorts =
                    (capacityShorts / 4)
                        .coerceAtLeast(chunkSamples)
                        .coerceAtMost(primeUpper)
                while (primedShorts < primeTargetShorts && running) {
                    val n = silence.size.coerceAtMost(primeTargetShorts - primedShorts)
                    val w = track.write(silence, 0, n)
                    if (w <= 0) break
                    primedShorts += w
                }
                track.play()
                var phaseUntilBeat = 0.0
                var beat = 0
                var clickPcm: ShortArray? = null
                var clickPos = -1
                var clickVol = 1f
                val work = ShortArray(chunkSamples)
                try {
                    while (running) {
                        work.fill(0)
                        for (i in 0 until chunkSamples) {
                            var acc = 0
                            if (clickPos >= 0 && clickPcm != null) {
                                val pcm = clickPcm!!
                                acc += (pcm[clickPos] * clickVol).roundToInt()
                                clickPos++
                                if (clickPos >= pcm.size) {
                                    clickPos = -1
                                    clickPcm = null
                                }
                            }
                            while (phaseUntilBeat <= 0.0 && running) {
                                val idx = beat % period
                                val tier = metronomeAccent(idx, noteDivisor)
                                val rawId = rawResId(preset, tier)
                                val pcmNew = pcmByRawId[rawId] ?: ShortArray(0)
                                clickPcm = pcmNew
                                clickVol = volumeForTier(tier)
                                clickPos = 0
                                if (pcmNew.isNotEmpty()) {
                                    acc += (pcmNew[0] * clickVol).roundToInt()
                                    clickPos = 1
                                    if (clickPos >= pcmNew.size) {
                                        clickPos = -1
                                        clickPcm = null
                                    }
                                }
                                mainHandler.post { onBeat(idx, tier) }
                                beat++
                                phaseUntilBeat += intervalSamples
                            }
                            phaseUntilBeat -= 1.0
                            work[i] =
                                acc.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                        var woff = 0
                        while (woff < chunkSamples && running) {
                            val w = track.write(work, woff, chunkSamples - woff)
                            if (w == AudioTrack.ERROR_INVALID_OPERATION ||
                                w == AudioTrack.ERROR_BAD_VALUE ||
                                w == AudioTrack.ERROR_DEAD_OBJECT
                            ) {
                                break
                            }
                            if (w > 0) {
                                woff += w
                            } else {
                                break
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
                    if (pcmOutTrack === track) {
                        pcmOutTrack = null
                    }
                }
            }
    }

    actual fun stop() {
        running = false
        pcmOutTrack?.run {
            try {
                pause()
                flush()
            } catch (_: Exception) {
            }
        }
        loopFuture?.cancel(true)
        loopFuture = null
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        storedConfig = config
        val cb = storedOnBeat ?: return
        if (!running) return
        stopLoop()
        running = true
        start(config, cb)
    }

    actual suspend fun warmUp() {
        withContext(Dispatchers.IO) {
            val ctx = drumApplicationContext()
            for (p in MetronomeSoundPreset.entries) {
                ensurePcmDecodedForPreset(ctx, p)
            }
        }
    }

    actual fun release() {
        stop()
        executor.shutdownNow()
        executor = Executors.newSingleThreadExecutor()
        pcmByRawId.clear()
        storedOnBeat = null
        storedConfig = null
    }

    private fun stopLoop() {
        running = false
        pcmOutTrack?.run {
            try {
                pause()
                flush()
            } catch (_: Exception) {
            }
        }
        loopFuture?.cancel(true)
        loopFuture = null
    }

    private fun ensurePcmDecodedForPreset(ctx: Context, preset: MetronomeSoundPreset) {
        for (tier in MetronomeAccent.entries) {
            val id = rawResId(preset, tier)
            pcmByRawId.computeIfAbsent(id) {
                RawResourceMonoPcmDecoder.decodeMonoS16Resampled(ctx, id, METRONOME_PCM_SAMPLE_RATE)
            }
        }
    }

    private fun volumeForTier(tier: MetronomeAccent): Float =
        when (tier) {
            MetronomeAccent.Strong -> 1f
            MetronomeAccent.Medium -> 0.88f
            MetronomeAccent.Weak -> 0.72f
        }

    companion object {
        private const val METRONOME_PCM_SAMPLE_RATE = 48_000

        private const val PCM_BYTES_PER_MONO_FRAME = 2

        private const val MIN_CHUNK_SAMPLES = 128

        /** ~5.3ms @ 48k；再小容易 write 过频，收益有限。 */
        private const val MAX_CHUNK_SAMPLES = 256

        private fun rawResId(
            preset: MetronomeSoundPreset,
            tier: MetronomeAccent,
        ): Int =
            when (preset) {
                MetronomeSoundPreset.Tr707 ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.tr707_strong
                        MetronomeAccent.Medium -> R.raw.tr707_weak
                        MetronomeAccent.Weak -> R.raw.tr707_weak
                    }
            }
    }
}
