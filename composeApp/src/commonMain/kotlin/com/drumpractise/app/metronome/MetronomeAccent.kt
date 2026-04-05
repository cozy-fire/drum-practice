package com.drumpractise.app.metronome

enum class MetronomeAccent {
    Strong,
    Medium,
    Weak,
}

fun metronomeBeatPeriod(noteDivisor: Int): Int {
    val d = noteDivisor.coerceIn(1, 4)
    return 4 * d
}

fun metronomeAccent(indexInPeriod: Int, noteDivisor: Int): MetronomeAccent {
    val period = metronomeBeatPeriod(noteDivisor)
    if (period <= 0) return MetronomeAccent.Weak
    val step = period / 4
    val idx = indexInPeriod.mod(period)
    if (idx % step != 0) return MetronomeAccent.Weak
    if (idx == 0) return MetronomeAccent.Strong
    return MetronomeAccent.Medium
}
