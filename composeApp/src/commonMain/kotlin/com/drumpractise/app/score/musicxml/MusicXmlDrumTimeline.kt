package com.drumpractise.app.score.musicxml

/**
 * 打击乐谱时间线：从 MusicXML part 中解析非休止符的 unpitched 音符，支持 [backup] / [forward] / 和弦。
 * 同一起始 division 的声部会合并为一组；组内保留每个音符的 [instrumentId] 以便独立采样。
 */
data class DrumNoteHit(
    val displayStep: Char,
    val displayOctave: Int,
    /** MusicXML `<instrument id="P1-I38"/>`；缺失时为空串，播放走默认采样 */
    val instrumentId: String,
)

data class DrumHitGroup(
    val startDivisions: Int,
    val durationDivisions: Int,
    val notes: List<DrumNoteHit>,
)

data class ParsedDrumScore(
    val divisionsPerQuarter: Int,
    val groups: List<DrumHitGroup>,
) {
    val loopLengthDivisions: Int
        get() =
            if (groups.isEmpty()) {
                0
            } else {
                groups.maxOf { it.startDivisions + it.durationDivisions }
            }
}

object MusicXmlDrumTimelineParser {
    fun parse(musicXml: String): ParsedDrumScore {
        val trimmed = musicXml.trim()
        if (trimmed.isEmpty()) return ParsedDrumScore(4, emptyList())

        val part = extractFirstPart(trimmed) ?: return ParsedDrumScore(4, emptyList())

        var divisionsPerQuarter =
            Regex("""<divisions>\s*(\d+)\s*</divisions>""").find(part)?.groupValues?.get(1)?.toIntOrNull() ?: 4

        val groups = mutableListOf<DrumHitGroup>()
        var cursor = 0
        var lastHitStartDivisions = 0
        var searchPos = 0
        val tagPattern = Regex("""<(backup|forward|note)(\s|>)""")

        while (true) {
            val m = tagPattern.find(part, searchPos) ?: break
            val tagName = m.groupValues[1]
            val tagStart = m.range.first
            when (tagName) {
                "backup" -> {
                    val blockEnd = part.indexOf("</backup>", tagStart).takeIf { it >= 0 } ?: break
                    val dur =
                        Regex("""<duration>\s*(\d+)\s*</duration>""")
                            .find(part, tagStart)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0
                    cursor -= dur
                    searchPos = blockEnd + "</backup>".length
                }
                "forward" -> {
                    val blockEnd = part.indexOf("</forward>", tagStart).takeIf { it >= 0 } ?: break
                    val dur =
                        Regex("""<duration>\s*(\d+)\s*</duration>""")
                            .find(part, tagStart)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0
                    cursor += dur
                    searchPos = blockEnd + "</forward>".length
                }
                "note" -> {
                    val blockEnd = part.indexOf("</note>", tagStart).takeIf { it >= 0 } ?: break
                    val noteXml = part.substring(tagStart, blockEnd + "</note>".length)
                    searchPos = blockEnd + "</note>".length

                    Regex("""<divisions>\s*(\d+)\s*</divisions>""").find(noteXml)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        divisionsPerQuarter = it
                    }

                    val dur =
                        Regex("""<duration>\s*(\d+)\s*</duration>""")
                            .find(noteXml)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0
                    val isRest = noteXml.contains("<rest")
                    val hasChord = noteXml.contains("<chord")
                    val hit = parseDrumNoteHit(noteXml)

                    if (isRest) {
                        if (!hasChord) cursor += dur
                        continue
                    }
                    if (hit == null) {
                        if (!hasChord) cursor += dur
                        continue
                    }

                    if (hasChord) {
                        mergeOrAdd(groups, lastHitStartDivisions, dur, listOf(hit))
                    } else {
                        lastHitStartDivisions = cursor
                        mergeOrAdd(groups, cursor, dur, listOf(hit))
                        cursor += dur
                    }
                }
            }
        }

        val sorted = groups.sortedBy { it.startDivisions }
        return ParsedDrumScore(divisionsPerQuarter = divisionsPerQuarter, groups = sorted)
    }

    private fun mergeOrAdd(
        groups: MutableList<DrumHitGroup>,
        start: Int,
        dur: Int,
        hits: List<DrumNoteHit>,
    ) {
        val idx = groups.indexOfLast { it.startDivisions == start }
        if (idx >= 0) {
            val old = groups[idx]
            val newDur = maxOf(old.durationDivisions, dur)
            groups[idx] =
                old.copy(
                    durationDivisions = newDur,
                    notes = old.notes + hits,
                )
        } else {
            groups.add(DrumHitGroup(startDivisions = start, durationDivisions = dur, notes = hits))
        }
    }

    private fun parseDrumNoteHit(noteXml: String): DrumNoteHit? {
        val step =
            Regex("""<display-step>\s*([A-G])\s*</display-step>""").find(noteXml)?.groupValues?.get(1)?.firstOrNull()
                ?: return null
        val oct =
            Regex("""<display-octave>\s*(\d+)\s*</display-octave>""").find(noteXml)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null
        val instrumentId =
            Regex("""<instrument\s+id="([^"]+)"""").find(noteXml)?.groupValues?.get(1).orEmpty()
        return DrumNoteHit(displayStep = step, displayOctave = oct, instrumentId = instrumentId)
    }

    private fun extractFirstPart(xml: String): String? {
        val start = xml.indexOf("<part ")
        if (start < 0) return null
        val end = xml.indexOf("</part>", start)
        if (end < 0) return null
        return xml.substring(start, end)
    }
}
