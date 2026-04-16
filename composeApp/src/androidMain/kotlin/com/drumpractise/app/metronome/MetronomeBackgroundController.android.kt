package com.drumpractise.app.metronome

import android.content.Intent
import android.os.Build
import com.drumpractise.app.data.drumApplicationContext

actual object MetronomeBackgroundController {
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null

    actual fun start(config: MetronomeRunConfig) {
        val ctx = drumApplicationContext()
        val intent =
            Intent(ctx, MetronomeForegroundService::class.java).apply {
                action = MetronomeForegroundService.ACTION_START
                putExtra(MetronomeForegroundService.EXTRA_BPM, config.bpm)
                putExtra(MetronomeForegroundService.EXTRA_NOTE_DIVISOR, config.noteDivisor)
                putExtra(MetronomeForegroundService.EXTRA_PRESET_ORDINAL, config.preset.ordinal)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    actual fun stop() {
        val ctx = drumApplicationContext()
        val intent =
            Intent(ctx, MetronomeForegroundService::class.java).apply {
                action = MetronomeForegroundService.ACTION_STOP
            }
        ctx.startService(intent)
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        val ctx = drumApplicationContext()
        val intent =
            Intent(ctx, MetronomeForegroundService::class.java).apply {
                action = MetronomeForegroundService.ACTION_UPDATE
                putExtra(MetronomeForegroundService.EXTRA_BPM, config.bpm)
                putExtra(MetronomeForegroundService.EXTRA_NOTE_DIVISOR, config.noteDivisor)
                putExtra(MetronomeForegroundService.EXTRA_PRESET_ORDINAL, config.preset.ordinal)
            }
        ctx.startService(intent)
    }

    actual fun setOnBeatListener(onBeat: ((Int, MetronomeAccent) -> Unit)?) {
        storedOnBeat = onBeat
    }

    internal fun emitBeat(indexInPeriod: Int, tier: MetronomeAccent) {
        storedOnBeat?.invoke(indexInPeriod, tier)
    }
}

