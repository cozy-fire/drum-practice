package com.drumpractise.app.score.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicXmlScoreTopBar(
    onBack: () -> Unit,
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
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 8.dp),
            ) {
                IconButton(
                    onClick = onBpmMinus,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "-1 BPM")
                }
                TextButton(
                    onClick = onOpenBpmDialog,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = bpm.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(
                    onClick = onBpmPlus,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "+1 BPM")
                }

                Box {
                    TextButton(
                        onClick = { onDivisorMenuExpandedChange(true) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            painter = painterResource(divisorIcon(noteDivisor)),
                            contentDescription = "分拍",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified,
                        )
                        Spacer(Modifier.size(1.dp))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "选择分拍",
                            modifier = Modifier.size(18.dp),
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
                                        modifier = Modifier.size(18.dp),
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
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "停止" else "开始",
                    )
                }
            }
        },
        windowInsets = WindowInsets.statusBars,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}
