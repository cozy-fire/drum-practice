package com.drumpractise.app.practice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drumpractise.app.platform.LocalWindowLayoutInfo

/**
 * @param layoutScale 宽屏等场景下整体放大（1 = 默认，2 = 原尺寸两倍）。
 */
@Composable
fun PracticePlaybackFloatingBar(
    playing: Boolean,
    playPauseEnabled: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val isWidthLayout = LocalWindowLayoutInfo.current.isTabletWidth
    val layoutScale = if (isWidthLayout) 1.5f else 1f
    val s = layoutScale.coerceIn(1f, 4f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape((28f * s).dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = (3f * s).dp,
        shadowElevation = (8f * s).dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = (10f * s).dp, vertical = (6f * s).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((4f * s).dp),
        ) {
            val btn = (52f * s).dp
            val iconPlay = (30f * s).dp
            val iconStd = (28f * s).dp
            IconButton(
                onClick = onPlayPause,
                enabled = playPauseEnabled,
                modifier = Modifier.size(btn),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放",
                    modifier = Modifier.size(iconPlay),
                    tint = iconTint,
                )
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(btn),
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "停止",
                    modifier = Modifier.size(iconStd),
                    tint = iconTint,
                )
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(btn),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    modifier = Modifier.size(iconStd),
                    tint = iconTint,
                )
            }
        }
    }
}
