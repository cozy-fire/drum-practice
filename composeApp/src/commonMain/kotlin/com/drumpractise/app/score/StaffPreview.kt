package com.drumpractise.app.score

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun StaffPreview(
    musicXml: String,
    playbackHighlight: Boolean = false,
    modifier: Modifier = Modifier,
)
