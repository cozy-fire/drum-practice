package com.drumpractise.app.score.musicxml

import com.drumpractise.app.constance.MetronomeConst

/**
 * 单个 loop 内一次击打时刻（与 [MusicXmlDrumTimelineParser] 的组对齐），单位：输出采样（如 48 kHz）。
 * 和弦内各音可带不同 [dynamicsMul]。
 */
data class DrumScheduledHit(
    val instrumentId: String,
    val dynamicsMul: Float,
)

data class DrumScoreHitEvent(
    val offsetSamples: Double,
    val hits: List<DrumScheduledHit>,
)

/**
 * 谱面循环的样本长度与按时间排序的击打序列；供 Android [AudioTrack] 写出循环按采样时钟消费。
 */
data class DrumScorePlaybackSchedule(
    val loopLengthSamples: Double,
    val events: List<DrumScoreHitEvent>,
)

/**
 * 由解析结果与 BPM、采样率构造时间线；**samplesPerQuarter = sampleRate × 60 / bpm**，与节拍器四分音符时长一致。
 */
fun buildDrumScorePlaybackSchedule(
    parsed: ParsedDrumScore,
    bpm: Int,
    pcmSampleRate: Int,
    /** 非 null 时按谱面 [DrumNoteHit.isAccent] 区分强弱；null 则所有击打倍率为 1。 */
    weakNoteVolumeScale: Float? = null,
): DrumScorePlaybackSchedule? {
    if (parsed.groups.isEmpty()) return null
    val bpmClamped = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
    val divisions = parsed.divisionsPerQuarter.coerceAtLeast(1)
    val samplesPerQuarter = pcmSampleRate * 60.0 / bpmClamped
    val weakCoerced = weakNoteVolumeScale?.coerceIn(0.05f, 1f)
    val events =
        parsed.groups.map { group ->
            val offset = group.startDivisions.toDouble() / divisions * samplesPerQuarter
            val hits =
                group.notes.map { note ->
                    val mul =
                        when {
                            weakCoerced == null -> 1f
                            note.isAccent -> 1f
                            else -> weakCoerced
                        }
                    DrumScheduledHit(instrumentId = note.instrumentId, dynamicsMul = mul)
                }
            DrumScoreHitEvent(offsetSamples = offset, hits = hits)
        }.sortedWith(
            compareBy({ it.offsetSamples }, { evt -> evt.hits.joinToString { "${it.instrumentId}:${it.dynamicsMul}" } }),
        )
    val loopLength = parsed.loopLengthDivisions.toDouble() / divisions * samplesPerQuarter
    val loopCoerced = loopLength.coerceAtLeast(1.0)
    return DrumScorePlaybackSchedule(loopLengthSamples = loopCoerced, events = events)
}
