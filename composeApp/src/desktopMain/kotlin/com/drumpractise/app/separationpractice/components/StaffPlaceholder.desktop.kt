package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun StaffPlaceholder(modifier: Modifier) {
    // Desktop has no Android WebView; keep an empty placeholder box.
    Box(modifier = modifier)
}

