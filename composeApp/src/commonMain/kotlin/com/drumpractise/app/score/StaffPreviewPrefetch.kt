package com.drumpractise.app.score

/**
 * Pre-warms [StaffPreview] SVG cache on Android; no-op elsewhere.
 */
expect suspend fun prefetchStaffPreviewSvgCache(
    paths: List<String>,
    scalePercent: Int,
)
