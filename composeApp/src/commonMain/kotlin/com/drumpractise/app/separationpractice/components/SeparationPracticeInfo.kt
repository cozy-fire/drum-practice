package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SeparationPracticeInfo(
    bpm: Int,
    loopCount: Int,
    modeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InfoPill(text = "$bpm BPM")
        InfoPill(text = modeLabel)
        InfoPill(text = "循环 $loopCount 次")
    }
}

@Composable
private fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            modifier
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

