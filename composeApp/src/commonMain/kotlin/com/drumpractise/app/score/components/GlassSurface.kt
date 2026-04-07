package com.drumpractise.app.score.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class PaddingSpec(val horizontal: Dp, val vertical: Dp)

@Composable
internal fun GlassSurface(
    shape: Shape,
    baseColor: Color,
    baseBrush: Brush?,
    baseAlpha: Float,
    borderAlpha: Float,
    highlightAlpha: Float,
    noiseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingSpec = PaddingSpec(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val highlight =
        remember(highlightAlpha) {
            Brush.linearGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = highlightAlpha),
                        Color.Transparent,
                    ),
                start = Offset(0f, 0f),
                end = Offset(600f, 400f),
            )
        }

    val noiseSeed = remember { 7_913 }

    Box(
        modifier =
            modifier
                .clip(shape)
                .let { m ->
                    if (baseBrush != null) m.background(baseBrush) else m.background(baseColor.copy(alpha = baseAlpha))
                }
                .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
                .drawWithContent {
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
                            val a = noiseAlpha * (0.4f + 0.6f * v)
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
                .clickable(onClick = onClick)
                .padding(horizontal = contentPadding.horizontal, vertical = contentPadding.vertical),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
