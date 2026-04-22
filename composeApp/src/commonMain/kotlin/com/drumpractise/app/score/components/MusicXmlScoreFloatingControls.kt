package com.drumpractise.app.score.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import org.jetbrains.compose.resources.painterResource

@Composable
fun MusicXmlScoreFloatingControls(
    bpm: Int,
    onBpmMinus: () -> Unit,
    onBpmPlus: () -> Unit,
    onOpenBpmDialog: () -> Unit,
    noteDivisor: Int,
    onNoteDivisorChange: (Int) -> Unit,
    divisorMenuExpanded: Boolean,
    onDivisorMenuExpandedChange: (Boolean) -> Unit,
    playing: Boolean,
    onPlayingToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onBpmMinus,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "-1 BPM",
                    modifier = Modifier.size(28.dp),
                )
            }
            TextButton(
                onClick = onOpenBpmDialog,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                Text(
                    text = bpm.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onBpmPlus,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "+1 BPM",
                    modifier = Modifier.size(28.dp),
                )
            }

            Box {
                TextButton(
                    onClick = { onDivisorMenuExpandedChange(true) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        painter = painterResource(divisorIcon(noteDivisor)),
                        contentDescription = "分拍",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified,
                    )
                    Spacer(Modifier.size(2.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "选择分拍",
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = divisorMenuExpanded,
                    onDismissRequest = { onDivisorMenuExpandedChange(false) },
                ) {
                    listOf(1, 2, 4).forEach { div ->
                        DropdownMenuItem(
                            text = {
                                Icon(
                                    painter = painterResource(divisorIcon(div)),
                                    contentDescription = divisorLabel(div),
                                    modifier = Modifier.size(22.dp),
                                    tint = Color.Unspecified,
                                )
                            },
                            onClick = {
                                onNoteDivisorChange(div)
                                onDivisorMenuExpandedChange(false)
                            },
                        )
                    }
                }
            }

            IconButton(
                onClick = onPlayingToggle,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "暂停" else "开始",
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
