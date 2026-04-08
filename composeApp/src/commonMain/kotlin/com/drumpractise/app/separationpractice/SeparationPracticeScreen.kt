package com.drumpractise.app.separationpractice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drumpractise.app.separationpractice.components.SeparationPracticeCard
import com.drumpractise.app.separationpractice.components.SeparationPracticeInfo
import com.drumpractise.app.separationpractice.components.SeparationPracticeSettingsSheet
import com.drumpractise.app.separationpractice.components.glassCircleButton

@Composable
fun SeparationPracticeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }

    // UI-only state (edited in settings sheet)
    var bpm by remember { mutableIntStateOf(110) }
    var loopCount by remember { mutableIntStateOf(4) }
    var mode by remember { mutableStateOf(SeparationPracticeMode.Sequential) }
    var points by remember { mutableStateOf(setOf(1, 2)) }

    val cards =
        remember {
            listOf(
                SeparationCardUi(
                    title = "基础节奏型",
                    gradientStart = Color(0xFF5A2E84),
                    gradientEnd = Color(0xFF2B1655),
                ),
                SeparationCardUi(
                    title = "加花 (16ths)",
                    gradientStart = Color(0xFF224A88),
                    gradientEnd = Color(0xFF0F2347),
                ),
                SeparationCardUi(
                    title = "加花 (16ths)",
                    gradientStart = Color(0xFF224A88),
                    gradientEnd = Color(0xFF0F2347),
                ),
                SeparationCardUi(
                    title = "加花 (16ths)",
                    gradientStart = Color(0xFF224A88),
                    gradientEnd = Color(0xFF0F2347),
                ),
                SeparationCardUi(
                    title = "加花 (16ths)",
                    gradientStart = Color(0xFF224A88),
                    gradientEnd = Color(0xFF0F2347),
                ),
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SeparationPracticeInfo(
                bpm = bpm,
                loopCount = loopCount,
                modeLabel = mode.label,
                modifier = Modifier.fillMaxWidth(),
            )

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = this.maxWidth >= 600.dp
                if (!wide) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(cards) { card ->
                            SeparationPracticeCard(
                                title = card.title,
                                gradientStart = card.gradientStart,
                                gradientEnd = card.gradientEnd,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        cards.forEach { card ->
                            SeparationPracticeCard(
                                title = card.title,
                                gradientStart = card.gradientStart,
                                gradientEnd = card.gradientEnd,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // Floating toolbar (bottom-right)
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            glassCircleButton(
                onClick = { playing = !playing },
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            glassCircleButton(
                onClick = { settingsOpen = true },
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        SeparationPracticeSettingsSheet(
            open = settingsOpen,
            points = points,
            bpm = bpm,
            loopCount = loopCount,
            mode = mode,
            onPointsChange = { points = it },
            onBpmChange = { bpm = it },
            onLoopCountChange = { loopCount = it },
            onModeChange = { mode = it },
            onDismiss = { settingsOpen = false },
            onConfirm = { settingsOpen = false },
        )
    }
}

private data class SeparationCardUi(
    val title: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

enum class SeparationPracticeMode(val label: String) {
    Sequential("顺序练习"),
    Random("随机练习"),
}

