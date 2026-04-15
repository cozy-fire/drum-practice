package com.drumpractise.app.platform

import androidx.compose.runtime.Composable

/**
 * Android 13+ 通知权限申请（其它平台为空实现）。
 *
 * - [request] 为 true 时尝试触发申请（由各平台 actual 决定是否需要/是否可申请）。
 */
@Composable
expect fun PostNotificationsPermissionEffect(request: Boolean)

