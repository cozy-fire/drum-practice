package com.drumpractise.app.score

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun StaffPreview(
    musicXml: String,
    zoomScale: Float,
    modifier: Modifier = Modifier,
)

