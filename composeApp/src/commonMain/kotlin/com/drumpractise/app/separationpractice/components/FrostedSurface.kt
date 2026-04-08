package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

@Composable
fun FrostedSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val baseBrush =
        remember {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.08f)),
                start = Offset(0f, 0f),
                end = Offset(600f, 400f),
            )
        }
    val highlight =
        remember {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(600f, 400f),
            )
        }
    val noiseSeed = remember { 7_913 }

    Box(
        modifier =
            modifier
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .drawWithContent {
                    drawRect(brush = baseBrush)
                    drawContent()
                    drawRect(brush = highlight, blendMode = BlendMode.SrcOver)

                    val step = 9f
                    val r = 1.2f
                    var y = 0f
                    var row = 0
                    while (y < size.height) {
                        var x = 0f
                        var col = 0
                        while (x < size.width) {
                            val v = ((row * 131 + col * 197 + noiseSeed) % 100) / 100f
                            val a = 0.05f * (0.4f + 0.6f * v)
                            drawCircle(
                                color = Color.White.copy(alpha = a),
                                radius = r,
                                center = Offset(x + (v * 3f), y + ((1f - v) * 3f)),
                                style = Fill,
                            )
                            x += step
                            col++
                        }
                        y += step
                        row++
                    }
                }
                .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
                .padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

