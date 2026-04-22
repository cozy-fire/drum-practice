package com.drumpractise.app.practice.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
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
        AnimatedContent(
            targetState = beat1To4,
            transitionSpec = {
                val enterSpringFloat =
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                val enterSpringOffset =
                    spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                val exitTween = tween<Float>(durationMillis = 140)
                (
                    fadeIn(animationSpec = enterSpringFloat) +
                        scaleIn(initialScale = 0.82f, animationSpec = enterSpringFloat) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = enterSpringOffset,
                        )
                ).togetherWith(
                    fadeOut(animationSpec = exitTween) +
                        scaleOut(targetScale = 1.12f, animationSpec = exitTween),
                )
            },
            contentAlignment = Alignment.Center,
            label = "PracticeCountInDigit",
        ) { digit ->
            Text(
                text = digit.toString(),
                color = digitColor,
                fontSize = 120.sp,
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}
