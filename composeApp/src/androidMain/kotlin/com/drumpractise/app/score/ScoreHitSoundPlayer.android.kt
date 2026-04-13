package com.drumpractise.app.score

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import androidx.compose.runtime.Composable
import com.drumpractise.app.R
import com.drumpractise.app.constance.hitVolumeForInstrument
import com.drumpractise.app.data.drumApplicationContext
import com.drumpractise.app.metronome.RawResourceMonoPcmDecoder
import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.musicxml.buildDrumScorePlaybackSchedule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 与 random_practice 谱中 instrument id（如 P1-I38）对应；占位 raw 可替换为真实采样。
private val DRUM_INSTRUMENT_TO_RAW: Map<String, Int> =
    mapOf(
        "P1-I36" to R.raw.drum_kick_drum, // 底鼓
        "P1-I38" to R.raw.drum_snare, // 军鼓
        "P1-I42" to R.raw.drum_hi_hat_closed, // 踩镲（闭镲）
        "P1-I43" to R.raw.drum_hi_hat_opened, // 踩镲（shuffle 等节奏型谱中的踩镲声部）
        "P1-I48" to R.raw.drum_tom_1, // 通鼓（偏高音）
        "P1-I50" to R.raw.drum_tom_2, // 通鼓（中音）
        "P1-I52" to R.raw.drum_tom_3, // 通鼓（偏低音 / 落地侧）
    )

private const val DRUM_HIT_PCM_SAMPLE_RATE = 48_000
private const val PCM_BYTES_PER_MONO_FRAME = 2
private const val MIN_CHUNK_SAMPLES = 128
private const val MAX_CHUNK_SAMPLES = 256

private data class ActiveVoice(
    val pcm: ShortArray,
    var pos: Int,
    val vol: Float,
)

private val globalAndroidScoreHitSoundPlayer by lazy { AndroidScoreHitSoundPlayer() }

actual fun globalScoreHitSoundPlayer(): ScoreHitSoundPlayer = globalAndroidScoreHitSoundPlayer

@Composable
actual fun rememberScoreHitSoundPlayer(): ScoreHitSoundPlayer = globalScoreHitSoundPlayer()

private class AndroidScoreHitSoundPlayer() : ScoreHitSoundPlayer {
    private var executor = Executors.newSingleThreadExecutor()
    private var loopFuture: Future<*>? = null

    @Volatile
    private var running = false

    @Volatile
    private var pcmOutTrack: AudioTrack? = null

    private val pcmByRawId = ConcurrentHashMap<Int, ShortArray>()
    private val warmedUp = AtomicBoolean(false)

    /** 每次 [stopPlayback] 或新一段 [startPlaybackFinite] 递增；旧 executor 任务 finally 中不得清掉新一段的 [running]。 */
    private val playbackGeneration = AtomicLong(0L)

    override val supportsFiniteCompletion: Boolean = true

    override suspend fun warmup() =
        withContext(Dispatchers.IO) {
            if (!warmedUp.compareAndSet(false, true)) return@withContext
            val ctx = drumApplicationContext()
            val ids =
                buildSet {
                    addAll(DRUM_INSTRUMENT_TO_RAW.values)
                    add(R.raw.tr707_weak)
                }
            for (rawId in ids) {
                pcmByRawId.computeIfAbsent(rawId) {
                    RawResourceMonoPcmDecoder.decodeMonoS16Resampled(ctx, it, DRUM_HIT_PCM_SAMPLE_RATE)
                }
            }
        }

    override fun startPlayback(musicXml: String, bpm: Int, weakNoteVolumeScale: Float?) {
        startPlaybackFinite(
            musicXml = musicXml,
            bpm = bpm,
            loopCount = Int.MAX_VALUE,
            onCompleted = null,
            weakNoteVolumeScale = weakNoteVolumeScale,
        )
    }

    override fun startPlaybackFinite(
        musicXml: String,
        bpm: Int,
        loopCount: Int,
        onCompleted: (() -> Unit)?,
        weakNoteVolumeScale: Float?,
    ) {
        stopPlayback()
        val xml = musicXml.trim()
        if (xml.isEmpty()) return

        val loops = loopCount.coerceAtLeast(1)
        val myGeneration = playbackGeneration.incrementAndGet()
        running = true
        loopFuture =
            executor.submit {
                try {
                val parsed = MusicXmlDrumTimelineParser.parse(xml)
                val schedule =
                    buildDrumScorePlaybackSchedule(
                        parsed,
                        bpm,
                        DRUM_HIT_PCM_SAMPLE_RATE,
                        weakNoteVolumeScale = weakNoteVolumeScale,
                    )
                        ?: run {
                            if (playbackGeneration.get() == myGeneration) {
                                running = false
                            }
                            return@submit
                        }

                val minBytes =
                    AudioTrack.getMinBufferSize(
                        DRUM_HIT_PCM_SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                if (minBytes <= 0) {
                    if (playbackGeneration.get() == myGeneration) {
                        running = false
                    }
                    return@submit
                }
                val bufferBytes = minBytes
                val attrBuilder =
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applyLowLatencyAudioFlag(attrBuilder)
                }
                val attrs = attrBuilder.build()
                val audioFormat =
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(DRUM_HIT_PCM_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                val trackBuilder =
                    AudioTrack
                        .Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferBytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                val track = trackBuilder.build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    if (playbackGeneration.get() == myGeneration) {
                        running = false
                    }
                    return@submit
                }
                pcmOutTrack = track
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val capacityShorts = bufferBytes / PCM_BYTES_PER_MONO_FRAME
                val chunkSamples =
                    (capacityShorts / 8).coerceIn(MIN_CHUNK_SAMPLES, MAX_CHUNK_SAMPLES)
                val silence = ShortArray(chunkSamples)
                var primedShorts = 0
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

                val events = schedule.events
                val loopLen = schedule.loopLengthSamples
                val work = ShortArray(chunkSamples)
                val active = ArrayList<ActiveVoice>(32)

                var timeInLoop = 0.0
                var nextEventIndex = 0
                var loopsCompleted = 0
                var completedNaturally = false

                try {
                    while (running) {
                        if (loopsCompleted >= loops) {
                            completedNaturally = true
                            break
                        }
                        work.fill(0)
                        for (si in 0 until chunkSamples) {
                            if (!running) break

                            while (nextEventIndex < events.size && events[nextEventIndex].offsetSamples <= timeInLoop + 1e-6) {
                                val evt = events[nextEventIndex]
                                for (h in evt.hits) {
                                    val ins = h.instrumentId
                                    val rid = rawForInstrument(ins)
                                    val pcm = pcmByRawId[rid] ?: continue
                                    if (pcm.isNotEmpty()) {
                                        val vol = hitVolumeForInstrument(ins) * h.dynamicsMul
                                        active.add(ActiveVoice(pcm, 0, vol))
                                    }
                                }
                                nextEventIndex++
                            }

                            var acc = 0
                            val it = active.iterator()
                            while (it.hasNext()) {
                                val v = it.next()
                                if (v.pos >= v.pcm.size) {
                                    it.remove()
                                    continue
                                }
                                acc += (v.pcm[v.pos] * v.vol).roundToInt()
                                v.pos++
                                if (v.pos >= v.pcm.size) {
                                    it.remove()
                                }
                            }
                            work[si] =
                                acc.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                            timeInLoop += 1.0
                            if (timeInLoop >= loopLen) {
                                timeInLoop -= loopLen
                                nextEventIndex = 0
                                loopsCompleted++
                                if (loopsCompleted >= loops) {
                                    break
                                }
                            }
                        }
                        var woff = 0
                        while (woff < chunkSamples && running) {
                            val w = track.write(work, woff, chunkSamples - woff)
                            if (w == AudioTrack.ERROR_INVALID_OPERATION ||
                                w == AudioTrack.ERROR_BAD_VALUE ||
                                w == AudioTrack.ERROR_DEAD_OBJECT
                            ) {
                                if (playbackGeneration.get() == myGeneration) {
                                    running = false
                                }
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
                    // Ensure visible stop for any waiting coroutine
                    val stillCurrent = playbackGeneration.get() == myGeneration
                    if (stillCurrent) {
                        val shouldNotify = completedNaturally && running
                        running = false
                        if (shouldNotify) {
                            try {
                                onCompleted?.invoke()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
                } catch (t: Throwable) {
                    if (playbackGeneration.get() == myGeneration) {
                        running = false
                    }
                }
            }
    }

    override fun stopPlayback() {
        playbackGeneration.incrementAndGet()
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

    fun release() {
        stopPlayback()
        executor.shutdownNow()
        executor = Executors.newSingleThreadExecutor()
        warmedUp.set(false)
        pcmByRawId.clear()
    }

    private fun rawForInstrument(instrumentId: String): Int =
        when {
            instrumentId.isEmpty() -> R.raw.tr707_weak
            else -> DRUM_INSTRUMENT_TO_RAW[instrumentId] ?: R.raw.tr707_weak
        }

    companion object {
        @Suppress("DEPRECATION")
        private fun applyLowLatencyAudioFlag(builder: AudioAttributes.Builder) {
            builder.setFlags(AudioAttributes.FLAG_LOW_LATENCY)
        }
    }
}
