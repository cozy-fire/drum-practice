package com.drumpractise.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        val app = UIApplication.sharedApplication
        if (enabled) {
            app.idleTimerDisabled = true
        }
        onDispose {
            if (enabled) {
                app.idleTimerDisabled = false
            }
        }
    }
}
