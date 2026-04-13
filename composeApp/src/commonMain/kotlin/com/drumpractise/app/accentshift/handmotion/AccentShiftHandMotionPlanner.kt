package com.drumpractise.app.accentshift.handmotion

import com.drumpractise.app.accentshift.model.AccentShiftItem
import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.musicxml.ParsedDrumScore

/**
 * 重音移位手型时间轴：RLRL 交替，左右手各自用「当前击打 × 该手下一记击打」映射四态。
 *
 * 与播放对齐：先将 [items] 按 [cardLoopCount] 展开为扁平队列（每项连续重复多遍再接下一项），
 * 再顺序拼接各卡时间轴；与 [com.drumpractise.app.score.ScorePlaybackController.playQueue] 的
 * 每卡 `loopCount` 一致。单手最后一击的「下一记」来自展开队列中**下一有效谱**的第一记（右手看 group0、
 * 左手看 group1）；若已是队列末尾则该手最后一记的下一记按 **非重音** 参与映射。
 */
object AccentShiftHandMotionPlanner {

    private val inactiveHandPlaceholder: HandStrokeKind = HandStrokeKind.Idle

    /**
     * @param cardLoopCount 与练习单卡循环圈数一致；小于 1 当作 1。
     */
    suspend fun buildHandMotionTimelineForQueue(
        items: List<AccentShiftItem>,
        bpm: Int,
        cardLoopCount: Int,
        loadXml: suspend (String) -> String,
    ): HandMotionTimeline {
        if (items.isEmpty()) return HandMotionTimeline(emptyList())
        val loops = cardLoopCount.coerceAtLeast(1)
        val expanded = items.flatMap { item -> List(loops) { item } }
        val all = ArrayList<HandMotionSegment>()
        for (i in expanded.indices) {
            val xml = loadXml(expanded[i].musicXmlPath).trim()
            if (xml.isEmpty()) continue
            val parsed = MusicXmlDrumTimelineParser.parse(xml)
            val nextParsed = loadNextNonEmptyParsed(expanded, startIndex = i, loadXml = loadXml)
            all.addAll(
                buildHandMotionTimelineForParsed(parsed, bpm, nextParsed = nextParsed).segments,
            )
        }
        return HandMotionTimeline(all)
    }

    /**
     * @param nextParsed 展开队列中当前谱之后的下一有效乐谱；无则单手最后一记的下一记按非重音处理。
     */
    fun buildHandMotionTimelineForParsed(
        parsed: ParsedDrumScore,
        bpm: Int,
        nextParsed: ParsedDrumScore?,
    ): HandMotionTimeline {
        val groups = parsed.groups.sortedBy { it.startDivisions }
        if (groups.isEmpty()) return HandMotionTimeline(emptyList())

        val bpmClamped = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
        val divisions = parsed.divisionsPerQuarter.coerceAtLeast(1)
        val msPerQuarter = 60000.0 / bpmClamped
        val msPerDivision = msPerQuarter / divisions

        val rightAccents = ArrayList<Boolean>()
        val leftAccents = ArrayList<Boolean>()
        for (i in groups.indices) {
            val accented = groups[i].notes.any { it.isAccent }
            if (i % 2 == 0) {
                rightAccents.add(accented)
            } else {
                leftAccents.add(accented)
            }
        }

        val lastRightNext =
            nextParsed?.let { p ->
                p.groups.getOrNull(0)?.notes?.any { it.isAccent } == true
            } ?: false
        val lastLeftNext =
            nextParsed?.let { p ->
                p.groups.getOrNull(1)?.notes?.any { it.isAccent } == true
            } ?: false

        val rightKinds = buildKindsForHand(rightAccents, lastNextAccent = lastRightNext)
        val leftKinds = buildKindsForHand(leftAccents, lastNextAccent = lastLeftNext)

        val segments = ArrayList<HandMotionSegment>(groups.size)
        var rightIdx = 0
        var leftIdx = 0
        for (i in groups.indices) {
            val g = groups[i]
            val durationMs =
                (g.durationDivisions * msPerDivision).toLong().coerceAtLeast(1L)
            val state =
                if (i % 2 == 0) {
                    val k = rightKinds[rightIdx++]
                    HandPairState(left = inactiveHandPlaceholder, right = k)
                } else {
                    val k = leftKinds[leftIdx++]
                    HandPairState(left = k, right = inactiveHandPlaceholder)
                }
            segments.add(HandMotionSegment(state = state, durationMs = durationMs))
        }
        return HandMotionTimeline(segments)
    }

    private fun buildKindsForHand(
        accents: List<Boolean>,
        lastNextAccent: Boolean,
    ): List<HandStrokeKind> {
        if (accents.isEmpty()) return emptyList()
        val n = accents.size
        return List(n) { j ->
            val cur = accents[j]
            val next =
                if (j < n - 1) {
                    accents[j + 1]
                } else {
                    lastNextAccent
                }
            mapAccentPairToKind(currentAccent = cur, nextAccent = next)
        }
    }

    /**
     * 从 [expanded] 中 [startIndex] 之后向前查找第一个非空 XML，解析后作为下一谱衔接；无则 null。
     */
    private suspend fun loadNextNonEmptyParsed(
        expanded: List<AccentShiftItem>,
        startIndex: Int,
        loadXml: suspend (String) -> String,
    ): ParsedDrumScore? {
        for (j in (startIndex + 1)..expanded.lastIndex) {
            val xml = loadXml(expanded[j].musicXmlPath).trim()
            if (xml.isEmpty()) continue
            return MusicXmlDrumTimelineParser.parse(xml)
        }
        return null
    }

    internal fun mapAccentPairToKind(
        currentAccent: Boolean,
        nextAccent: Boolean,
    ): HandStrokeKind =
        when {
            currentAccent && !nextAccent -> HandStrokeKind.Down
            !currentAccent && !nextAccent -> HandStrokeKind.Tap
            !currentAccent && nextAccent -> HandStrokeKind.Up
            currentAccent && nextAccent -> HandStrokeKind.Full
            else -> HandStrokeKind.Tap
        }
}
