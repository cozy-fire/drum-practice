package com.drumpractise.app.score

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun StaffPreview(
    musicXml: String,
    playbackHighlight: Boolean = false,
    modifier: Modifier = Modifier,
    /** When set (e.g. resource path under composeResources/files/), Android SVG cache uses a stable key. */
    staffPreviewCacheKey: String? = null,
)
