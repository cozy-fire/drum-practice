package com.drumpractise.app.platform

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WindowLayoutInfo(
    val isTabletWidth: Boolean,
    val windowWidth: Dp,
)

val LocalWindowLayoutInfo =
    staticCompositionLocalOf {
        WindowLayoutInfo(
            isTabletWidth = false,
            windowWidth = 0.dp,
        )
    }

