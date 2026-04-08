package com.drumpractise.app.separationpractice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drumpractise.app.separationpractice.SeparationPracticeMode

@Composable
fun SeparationPracticeSettingsSheet(
    open: Boolean,
    points: Set<Int>,
    bpm: Int,
    loopCount: Int,
    mode: SeparationPracticeMode,
    onPointsChange: (Set<Int>) -> Unit,
    onBpmChange: (Int) -> Unit,
    onLoopCountChange: (Int) -> Unit,
    onModeChange: (SeparationPracticeMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!open) return

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onDismiss),
        )

        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                        .widthIn(min = 320.dp, max = 420.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape)
                        .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = true).verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("练习设置", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭")
                        }
                    }

                    SectionTitle("练习点位")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        (1..4).forEach { idx ->
                            val checked = points.contains(idx)
                            SelectRow(
                                checked = checked,
                                label = "点位 $idx",
                                onToggle = {
                                    val next = if (checked) points - idx else points + idx
                                    onPointsChange(next)
                                },
                            )
                        }
                    }

                    SectionTitle("循环次数")
                    StepperRow(
                        valueText = "${loopCount.coerceAtLeast(1)} 次",
                        onMinus = { onLoopCountChange((loopCount - 1).coerceAtLeast(1)) },
                        onPlus = { onLoopCountChange((loopCount + 1).coerceAtMost(99)) },
                    )

                    SectionTitle("节拍速度 (BPM)")
                    StepperRow(
                        valueText = "${bpm.coerceIn(40, 300)} BPM",
                        onMinus = { onBpmChange((bpm - 1).coerceIn(40, 300)) },
                        onPlus = { onBpmChange((bpm + 1).coerceIn(40, 300)) },
                    )
                    Slider(
                        value = bpm.toFloat().coerceIn(40f, 300f),
                        onValueChange = { onBpmChange(it.toInt().coerceIn(40, 300)) },
                        valueRange = 40f..300f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SectionTitle("练习模式")
                    ModeRow(mode = mode, onModeChange = onModeChange)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text("确认设置")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SelectRow(
    checked: Boolean,
    label: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    shape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
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
        Text(valueText, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
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
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ModeRow(
    mode: SeparationPracticeMode,
    onModeChange: (SeparationPracticeMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModePill(
            text = SeparationPracticeMode.Sequential.label,
            selected = mode == SeparationPracticeMode.Sequential,
            onClick = { onModeChange(SeparationPracticeMode.Sequential) },
        )
        ModePill(
            text = SeparationPracticeMode.Random.label,
            selected = mode == SeparationPracticeMode.Random,
            onClick = { onModeChange(SeparationPracticeMode.Random) },
        )
    }
}

@Composable
private fun ModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    shape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

