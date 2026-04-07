package com.drumpractise.app.score.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class TopActionButtonStyle {
    Gray,
    GradientPurpleBlue,
}

@Composable
internal fun TopActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    style: TopActionButtonStyle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val dark = isSystemInDarkTheme()

    val baseColor: Color
    val baseBrush: Brush?
    val baseAlpha: Float
    val borderAlpha: Float
    val highlightAlpha: Float
    val noiseAlpha: Float

    when (style) {
        TopActionButtonStyle.Gray -> {
            baseBrush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color(0x5E4C4C53).copy(alpha = 0.95f),
                            Color(0x5E4C4C53).copy(alpha = 0.95f),
                        ),
                )
            baseColor = Color.Transparent
            baseAlpha = 0f
            borderAlpha = if (dark) 0.22f else 0.14f
            highlightAlpha = if (dark) 0.14f else 0.10f
            noiseAlpha = if (dark) 0.018f else 0.010f
        }

        TopActionButtonStyle.GradientPurpleBlue -> {
            baseBrush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color(0xFF9B4DFF).copy(alpha = 0.95f),
                            Color(0xFF3B82F6).copy(alpha = 0.95f),
                        ),
                )
            baseColor = Color.Transparent
            baseAlpha = 0f
            borderAlpha = if (dark) 0.22f else 0.14f
            highlightAlpha = if (dark) 0.14f else 0.10f
            noiseAlpha = if (dark) 0.018f else 0.010f
        }
    }

    GlassSurface(
        modifier = modifier,
        shape = shape,
        baseColor = baseColor,
        baseBrush = baseBrush,
        baseAlpha = baseAlpha,
        borderAlpha = borderAlpha,
        highlightAlpha = highlightAlpha,
        noiseAlpha = noiseAlpha,
        onClick = onClick,
        contentPadding = PaddingSpec(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
