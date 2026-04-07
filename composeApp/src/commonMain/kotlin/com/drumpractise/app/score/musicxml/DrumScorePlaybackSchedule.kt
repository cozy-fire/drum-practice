package com.drumpractise.app.score.musicxml

/**
 * 单个 loop 内一次击打时刻（与 [MusicXmlDrumTimelineParser] 的组对齐），单位：输出采样（如 48 kHz）。
 */
data class DrumScoreHitEvent(
    val offsetSamples: Double,
    val instrumentIds: List<String>,
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
): DrumScorePlaybackSchedule? {
    if (parsed.groups.isEmpty()) return null
    val bpmClamped = bpm.coerceIn(10, 300)
    val divisions = parsed.divisionsPerQuarter.coerceAtLeast(1)
    val samplesPerQuarter = pcmSampleRate * 60.0 / bpmClamped
    val events =
        parsed.groups.map { group ->
            val offset = group.startDivisions.toDouble() / divisions * samplesPerQuarter
            val ids = group.notes.map { it.instrumentId }
            DrumScoreHitEvent(offsetSamples = offset, instrumentIds = ids)
        }.sortedWith(compareBy({ it.offsetSamples }, { it.instrumentIds.joinToString() }))
    val loopLength = parsed.loopLengthDivisions.toDouble() / divisions * samplesPerQuarter
    val loopCoerced = loopLength.coerceAtLeast(1.0)
    return DrumScorePlaybackSchedule(loopLengthSamples = loopCoerced, events = events)
}
