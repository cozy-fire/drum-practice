package com.drumpractise.app.metronome

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.LockSupport
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.SwingUtilities
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

actual class MetronomeEngine actual constructor() {
    private var executor = Executors.newSingleThreadExecutor()
    private var loopFuture: Future<*>? = null
    @Volatile private var running = false
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null

    actual fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit) {
        stopLoop()
        storedOnBeat = onBeat
        running = true
        val intervalNs = metronomeIntervalNs(config.bpm, config.noteDivisor)
        val preset = config.preset
        val period = metronomeBeatPeriod(config.noteDivisor)
        loopFuture =
            executor.submit {
                var beat = 0
                var nextDeadlineNs = System.nanoTime()
                while (running) {
                    var remaining = nextDeadlineNs - System.nanoTime()
                    while (remaining > 0L && running) {
                        LockSupport.parkNanos(remaining)
                        remaining = nextDeadlineNs - System.nanoTime()
                    }
                    if (Thread.interrupted()) {
                        Thread.interrupted()
                        break
                    }
                    if (!running) break
                    val indexInPeriod = beat % period
                    val tier = metronomeAccent(indexInPeriod, config.noteDivisor)
                    playBeep(preset, tier)
                    SwingUtilities.invokeLater { onBeat(indexInPeriod, tier) }
                    beat++
                    nextDeadlineNs += intervalNs
                }
            }
    }

    actual fun stop() {
        running = false
        loopFuture?.cancel(true)
        loopFuture = null
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        val cb = storedOnBeat ?: return
        if (!running) return
        stopLoop()
        running = true
        start(config, cb)
    }

    actual suspend fun warmUp() {}

    actual fun release() {
        stop()
        executor.shutdownNow()
        executor = Executors.newSingleThreadExecutor()
        storedOnBeat = null
    }

    private fun stopLoop() {
        running = false
        loopFuture?.cancel(true)
        loopFuture = null
    }

    @Suppress("MagicNumber")
    private fun playBeep(preset: MetronomeSoundPreset, tier: MetronomeAccent) {
        val freq =
            when (preset) {
                MetronomeSoundPreset.Tr707 ->
                    when (tier) {
                        MetronomeAccent.Strong -> 920.0
                        MetronomeAccent.Medium -> 620.0
                        MetronomeAccent.Weak -> 620.0
                    }
            }
        val durationMs =
            when (tier) {
                MetronomeAccent.Strong -> 90
                MetronomeAccent.Medium -> 75
                MetronomeAccent.Weak -> 60
            }
        val amp =
            when (tier) {
                MetronomeAccent.Strong -> 0.35
                MetronomeAccent.Medium -> 0.26
                MetronomeAccent.Weak -> 0.18
            }
        try {
            val sampleRate = 44100f
            val numSamples = sampleRate * durationMs / 1000f
            val samples = ShortArray(numSamples.roundToInt())
            var i = 0
            while (i < samples.size) {
                val t = i / sampleRate
                val env = 1.0 - (i.toDouble() / samples.size)
                samples[i] = (sin(2 * PI * freq * t) * amp * env * Short.MAX_VALUE).roundToInt().toShort()
                i++
            }
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val clip = AudioSystem.getLine(javax.sound.sampled.DataLine.Info(Clip::class.java, format)) as Clip
            clip.open(format, samples.toByteArrayLittleEndian(), 0, samples.size * 2)
            clip.start()
        } catch (_: Exception) {
        }
    }

    private fun ShortArray.toByteArrayLittleEndian(): ByteArray {
        val out = ByteArray(size * 2)
        var o = 0
        for (s in this) {
            out[o++] = (s.toInt() and 0xff).toByte()
            out[o++] = (s.toInt() shr 8 and 0xff).toByte()
        }
        return out
    }
}
