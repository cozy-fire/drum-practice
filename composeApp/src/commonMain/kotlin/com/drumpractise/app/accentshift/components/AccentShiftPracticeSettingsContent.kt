package com.drumpractise.app.accentshift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drumpractise.app.accentshift.AccentShiftPracticeColors
import com.drumpractise.app.accentshift.model.AccentShiftPracticeState
import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

@Composable
fun AccentShiftPracticeSettingsContent(
    practiceState: AccentShiftPracticeState,
    onPracticeStateChange: (AccentShiftPracticeState) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val params = practiceState.currentParams()
    Column(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("练习设置", style = MaterialTheme.typography.titleLarge, color = Color.White)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
                }
            }

            SectionTitle("列表循环次数（当前档位）")
            StepperRow(
                valueText = "${params.listLoopCount.coerceAtLeast(1)} 次",
                onMinus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(listLoopCount = (it.listLoopCount - 1).coerceAtLeast(1)) },
                    )
                },
                onPlus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(listLoopCount = (it.listLoopCount + 1).coerceAtMost(99)) },
                    )
                },
            )

            SectionTitle("单个卡片循环次数（当前档位）")
            StepperRow(
                valueText = "${params.cardLoopCount.coerceAtLeast(1)} 次",
                onMinus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(cardLoopCount = (it.cardLoopCount - 1).coerceAtLeast(1)) },
                    )
                },
                onPlus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(cardLoopCount = (it.cardLoopCount + 1).coerceAtMost(99)) },
                    )
                },
            )

            SectionTitle("节拍速度 (BPM)（当前档位）")
            StepperRow(
                valueText = "${params.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)} BPM",
                onMinus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(bpm = (it.bpm - 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)) },
                    )
                },
                onPlus = {
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { it.copy(bpm = (it.bpm + 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)) },
                    )
                },
            )
            Slider(
                value = params.bpm.toFloat().coerceIn(MetronomeConst.BPM_MIN.toFloat(), MetronomeConst.BPM_MAX.toFloat()),
                onValueChange = { v ->
                    onPracticeStateChange(
                        practiceState.updateCurrentTier { t ->
                            t.copy(bpm = v.toInt().coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX))
                        },
                    )
                },
                valueRange = MetronomeConst.BPM_MIN.toFloat()..MetronomeConst.BPM_MAX.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("练习模式（当前档位）")
            ModeRow(
                mode = params.mode,
                onModeChange = { mode ->
                    onPracticeStateChange(practiceState.updateCurrentTier { it.copy(mode = mode) })
                },
            )
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("确认设置")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.9f),
    )
}

@Composable
private fun StepperRow(
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(icon = Icons.Filled.Remove, onClick = onMinus)
        Text(valueText, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        StepperButton(icon = Icons.Filled.Add, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.10f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun ModeRow(
    mode: SeparationPracticeMode,
    onModeChange: (SeparationPracticeMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeItem(
            text = SeparationPracticeMode.Sequential.label,
            selected = mode == SeparationPracticeMode.Sequential,
            onClick = { onModeChange(SeparationPracticeMode.Sequential) },
        )
        ModeItem(
            text = SeparationPracticeMode.Random.label,
            selected = mode == SeparationPracticeMode.Random,
            onClick = { onModeChange(SeparationPracticeMode.Random) },
        )
    }
}

@Composable
private fun ModeItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier =
            Modifier
                .background(
                    if (selected) AccentShiftPracticeColors.accent.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.10f),
                    shape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
