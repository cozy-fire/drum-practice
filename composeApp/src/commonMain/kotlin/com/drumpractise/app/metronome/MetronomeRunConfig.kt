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

/** 一拍时长（纳秒），由 BPM 与分拍直接算，避免先取整毫秒再乘 10⁶ 的二次误差。 */
fun metronomeIntervalNs(bpm: Int, noteDivisor: Int): Long {
    val safeBpm = bpm.coerceIn(10, 300)
    val safeDiv = noteDivisor.coerceIn(1, 4)
    return kotlin.math.round(60_000_000_000.0 / safeBpm / safeDiv).toLong().coerceAtLeast(1L)
}
