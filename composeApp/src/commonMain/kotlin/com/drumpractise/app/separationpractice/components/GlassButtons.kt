package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun glassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color? = null,
    content: @Composable () -> Unit,
) {
    // Height=56dp, radius=28dp: corner radius equals half height.
    val height = 56.dp
    val shape = RoundedCornerShape(height / 2)
    if (baseColor == null) {
        FrostedSurface(
            shape = shape,
            onClick = onClick,
            modifier = modifier.size(height),
            content = content,
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(height)
                    .clip(shape)
                    .background(baseColor)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

