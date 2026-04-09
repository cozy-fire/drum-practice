package com.drumpractise.app.score

import com.drumpractise.app.score.musicxml.MusicXmlRepository
import kotlinx.coroutines.yield

actual suspend fun prefetchStaffPreviewSvgCache(
    paths: List<String>,
    scalePercent: Int,
) {
    for (path in paths) {
        val xml = MusicXmlRepository.getXml(path)
        if (xml.isNotBlank()) {
            StaffPreviewSvgCache.ensureRendered(path, xml, scalePercent)
        }
        yield()
    }
}
