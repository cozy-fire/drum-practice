package com.drumpractise.app.metronome

data class MetronomeRunConfig(
    val bpm: Int,
    val noteDivisor: Int,
    val preset: MetronomeSoundPreset,
)

fun metronomeIntervalMs(bpm: Int, noteDivisor: Int): Long {
    val safeBpm = bpm.coerceIn(10, 300)
    val safeDiv = noteDivisor.coerceIn(1, 4)
    return kotlin.math.round(60000.0 / safeBpm / safeDiv).toLong().coerceAtLeast(1L)
}
