package com.drumpractise.app.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun PostNotificationsPermissionEffect(request: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (!request) return

    val ctx = LocalContext.current
    val activity = ctx as? ComponentActivity ?: return
    val alreadyGranted =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    if (alreadyGranted) return

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // 这里不做状态持久化；权限的结果只影响通知可见性。
        }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

