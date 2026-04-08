package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

@Composable
fun SeparationPracticeSettingsContent(
    config: SeparationConfig,
    onConfigChange: (SeparationConfig) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("练习设置", style = MaterialTheme.typography.titleLarge, color = Color.White)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
            }
        }

        SectionTitle("练习点位")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..4).forEach { idx ->
                val checked = config.points.contains(idx)
                SelectRow(
                    checked = checked,
                    label = "点位 $idx",
                    onToggle = {
                        val next = if (checked) config.points - idx else config.points + idx
                        onConfigChange(config.copy(points = next))
                    },
                )
            }
        }

        SectionTitle("循环次数")
        StepperRow(
            valueText = "${config.loopCount.coerceAtLeast(1)} 次",
            onMinus = { onConfigChange(config.copy(loopCount = (config.loopCount - 1).coerceAtLeast(1))) },
            onPlus = { onConfigChange(config.copy(loopCount = (config.loopCount + 1).coerceAtMost(99))) },
        )

        SectionTitle("节拍速度 (BPM)")
        StepperRow(
            valueText = "${config.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)} BPM",
            onMinus = { onConfigChange(config.copy(bpm = (config.bpm - 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX))) },
            onPlus = { onConfigChange(config.copy(bpm = (config.bpm + 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX))) },
        )
        Slider(
            value = config.bpm.toFloat().coerceIn(MetronomeConst.BPM_MIN.toFloat(), MetronomeConst.BPM_MAX.toFloat()),
            onValueChange = { onConfigChange(config.copy(bpm = it.toInt().coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX))) },
            valueRange = MetronomeConst.BPM_MIN.toFloat()..MetronomeConst.BPM_MAX.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("练习模式")
        ModeRow(
            mode = config.mode,
            onModeChange = { onConfigChange(config.copy(mode = it)) },
        )

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
private fun SelectRow(
    checked: Boolean,
    label: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (checked) Color(0xFF7C3AED).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                    shape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.wrapContentWidth(),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C3AED), checkmarkColor = Color.White),
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(6) {
                Box(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(6.dp)),
                )
            }
        }
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
                    if (selected) Color(0xFF7C3AED).copy(alpha = 0.38f) else Color.White.copy(alpha = 0.10f),
                    shape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

