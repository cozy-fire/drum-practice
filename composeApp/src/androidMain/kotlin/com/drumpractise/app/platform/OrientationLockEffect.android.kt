package com.drumpractise.app.platform

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun OrientationLockEffect(isTabletWidth: Boolean) {
    val ctx = LocalContext.current
    val activity = ctx as? ComponentActivity ?: return

    DisposableEffect(activity, isTabletWidth) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation =
            if (isTabletWidth) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        onDispose {
            activity.requestedOrientation = previous
        }
    }
}

