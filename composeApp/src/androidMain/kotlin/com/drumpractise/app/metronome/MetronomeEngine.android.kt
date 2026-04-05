package com.drumpractise.app.metronome

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.drumpractise.app.data.drumApplicationContext
import com.drumpractise.app.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

actual class MetronomeEngine actual constructor() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var executor = Executors.newSingleThreadExecutor()
    private var loopFuture: Future<*>? = null
    @Volatile private var running = false
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null
    private var storedConfig: MetronomeRunConfig? = null

    private var soundPool: SoundPool? = null
    private val soundIds = Array(MetronomeSoundPreset.entries.size) { IntArray(MetronomeAccent.entries.size) }
    private val loadReady = AtomicBoolean(false)

    actual fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit) {
        stopLoop()
        storedOnBeat = onBeat
        storedConfig = config
        running = true
        val intervalMs = metronomeIntervalMs(config.bpm, config.noteDivisor)
        val presetOrdinal = config.preset.ordinal
        val period = metronomeBeatPeriod(config.noteDivisor)
        loopFuture =
            executor.submit {
                ensureSoundPoolLoadedBlocking()
                val pool = soundPool ?: return@submit
                var beat = 0
                var nextDeadlineNs = System.nanoTime()
                val intervalNs = intervalMs * 1_000_000L
                while (running) {
                    val now = System.nanoTime()
                    val sleepMs = max(0L, (nextDeadlineNs - now) / 1_000_000L)
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                    if (!running) break
                    val indexInPeriod = beat % period
                    val tier = metronomeAccent(indexInPeriod, config.noteDivisor)
                    val tierOrdinal = tier.ordinal
                    val sid = soundIds[presetOrdinal][tierOrdinal]
                    if (sid != 0) {
                        val vol =
                            when (tier) {
                                MetronomeAccent.Strong -> 1f
                                MetronomeAccent.Medium -> 0.88f
                                MetronomeAccent.Weak -> 0.72f
                            }
                        pool.play(sid, vol, vol, 1, 0, 1f)
                    }
                    val cb = onBeat
                    mainHandler.post { cb(indexInPeriod, tier) }
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
        storedConfig = config
        val cb = storedOnBeat ?: return
        if (!running) return
        stopLoop()
        running = true
        start(config, cb)
    }

    actual fun warmUp() {
        executor.execute { ensureSoundPoolLoadedBlocking() }
    }

    actual fun release() {
        stop()
        executor.shutdownNow()
        executor = Executors.newSingleThreadExecutor()
        soundPool?.release()
        soundPool = null
        loadReady.set(false)
        for (p in soundIds.indices) {
            soundIds[p].fill(0)
        }
        storedOnBeat = null
        storedConfig = null
    }

    private fun stopLoop() {
        running = false
        loopFuture?.cancel(true)
        loopFuture = null
    }

    private fun ensureSoundPoolLoadedBlocking() {
        if (loadReady.get() && soundPool != null) return
        synchronized(this) {
            if (loadReady.get() && soundPool != null) return
            val ctx = drumApplicationContext()
            val attrs =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            val pool =
                SoundPool
                    .Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(attrs)
                    .build()
            val latch = CountDownLatch(24)
            pool.setOnLoadCompleteListener { _, _, _ ->
                latch.countDown()
            }
            for (preset in MetronomeSoundPreset.entries) {
                for (tier in MetronomeAccent.entries) {
                    val raw = rawResId(preset, tier)
                    val id = pool.load(ctx, raw, 1)
                    soundIds[preset.ordinal][tier.ordinal] = id
                }
            }
            latch.await(15, TimeUnit.SECONDS)
            soundPool = pool
            loadReady.set(true)
        }
    }

    companion object {
        @Suppress("CyclomaticComplexMethod")
        private fun rawResId(
            preset: MetronomeSoundPreset,
            tier: MetronomeAccent,
        ): Int =
            when (preset) {
                MetronomeSoundPreset.ClickWood ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.clickwood_strong
                        MetronomeAccent.Medium -> R.raw.clickwood_medium
                        MetronomeAccent.Weak -> R.raw.clickwood_weak
                    }
                MetronomeSoundPreset.BeepHigh ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.beephigh_strong
                        MetronomeAccent.Medium -> R.raw.beephigh_medium
                        MetronomeAccent.Weak -> R.raw.beephigh_weak
                    }
                MetronomeSoundPreset.BeepLow ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.beeplow_strong
                        MetronomeAccent.Medium -> R.raw.beeplow_medium
                        MetronomeAccent.Weak -> R.raw.beeplow_weak
                    }
                MetronomeSoundPreset.Digital ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.digital_strong
                        MetronomeAccent.Medium -> R.raw.digital_medium
                        MetronomeAccent.Weak -> R.raw.digital_weak
                    }
                MetronomeSoundPreset.Bell ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.bell_strong
                        MetronomeAccent.Medium -> R.raw.bell_medium
                        MetronomeAccent.Weak -> R.raw.bell_weak
                    }
                MetronomeSoundPreset.SharpClick ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.sharpclick_strong
                        MetronomeAccent.Medium -> R.raw.sharpclick_medium
                        MetronomeAccent.Weak -> R.raw.sharpclick_weak
                    }
                MetronomeSoundPreset.WoodKnock ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.woodknock_strong
                        MetronomeAccent.Medium -> R.raw.woodknock_medium
                        MetronomeAccent.Weak -> R.raw.woodknock_weak
                    }
                MetronomeSoundPreset.SoftTick ->
                    when (tier) {
                        MetronomeAccent.Strong -> R.raw.softtick_strong
                        MetronomeAccent.Medium -> R.raw.softtick_medium
                        MetronomeAccent.Weak -> R.raw.softtick_weak
                    }
            }
    }
}
