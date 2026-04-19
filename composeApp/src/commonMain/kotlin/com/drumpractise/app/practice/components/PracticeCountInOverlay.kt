package com.drumpractise.app.practice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * 全屏拦截触摸；中央展示当前预备拍数字（1–4）。
 */
@Composable
fun PracticeCountInOverlay(
    beat1To4: Int,
    modifier: Modifier = Modifier,
    digitColor: Color = Color.White,
    scrimColor: Color = Color.Black.copy(alpha = 0.58f),
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = beat1To4.toString(),
            color = digitColor,
            fontSize = 120.sp,
            style = MaterialTheme.typography.displayLarge,
        )
    }
}
