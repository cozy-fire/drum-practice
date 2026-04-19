package com.drumpractise.app.metronome

import com.drumpractise.app.constance.MetronomeConst
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MetronomeRunConfig(
    val bpm: Int,
    val noteDivisor: Int,
    val preset: MetronomeSoundPreset,
)

enum class MetronomePracticeItem {
    Single,
    Double,
}

@Serializable
data class MetronomePracticeItemSnapshot(
    val bpm: Int = DEFAULT_BPM,
    val noteDivisor: Int = DEFAULT_NOTE_DIVISOR,
) {
    companion object {
        const val DEFAULT_BPM = 110
        const val DEFAULT_NOTE_DIVISOR = 1

        fun default(): MetronomePracticeItemSnapshot = MetronomePracticeItemSnapshot()
    }
}

@Serializable
data class MetronomePracticePersistState(
    val single: MetronomePracticeItemSnapshot = MetronomePracticeItemSnapshot.default(),
    @SerialName("double")
    val doubleStroke: MetronomePracticeItemSnapshot = MetronomePracticeItemSnapshot.default(),
) {
    companion object {
        fun default(): MetronomePracticePersistState = MetronomePracticePersistState()
    }
}

internal fun normalizeMetronomePracticeSnapshot(s: MetronomePracticeItemSnapshot): MetronomePracticeItemSnapshot =
    MetronomePracticeItemSnapshot(
        bpm = s.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
        noteDivisor = normalizeNoteDivisor(s.noteDivisor),
    )

internal fun normalizeNoteDivisor(v: Int): Int =
    when (v) {
        1, 2, 4 -> v
        else -> MetronomePracticeItemSnapshot.DEFAULT_NOTE_DIVISOR
    }

internal fun MetronomePracticePersistState.normalized(): MetronomePracticePersistState =
    MetronomePracticePersistState(
        single = normalizeMetronomePracticeSnapshot(single),
        doubleStroke = normalizeMetronomePracticeSnapshot(doubleStroke),
    )

/** 一拍时长（纳秒），由 BPM 与分拍直接算，避免先取整毫秒再乘 10⁶ 的二次误差。 */
fun metronomeIntervalNs(bpm: Int, noteDivisor: Int): Long {
    val safeBpm = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
    val safeDiv = noteDivisor.coerceIn(1, 4)
    return kotlin.math.round(60_000_000_000.0 / safeBpm / safeDiv).toLong().coerceAtLeast(1L)
}
