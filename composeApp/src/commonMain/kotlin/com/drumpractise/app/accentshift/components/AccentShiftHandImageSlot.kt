package com.drumpractise.app.accentshift.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.drumpractise.app.accentshift.AccentShiftPracticeColors
import org.jetbrains.compose.animatedimage.AnimatedImage
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadResourceAnimatedImage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 手型展示：静态位图或 GIF 动画（[AnimatedGif] 使用 JetBrains `components-animatedimage`）。
 *
 * @param resourcePath GIF 在 compose 资源中的路径，例如 `drawable/foo.gif`（与 [drumhero.composeapp.generated.resources.Res.readBytes] 的路径风格一致）。
 */
sealed class HandImageDisplayMode {
    data class Static(val drawable: DrawableResource) : HandImageDisplayMode()

    data class AnimatedGif(val resourcePath: String) : HandImageDisplayMode()
}

@Composable
fun AccentShiftHandImageSlot(
    mode: HandImageDisplayMode,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .semantics { contentDescription = accessibilityDescription }
                .clip(RoundedCornerShape(14.dp)),
        color = AccentShiftPracticeColors.surfaceCard,
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (mode) {
                is HandImageDisplayMode.Static -> {
                    Image(
                        painter = painterResource(mode.drawable),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                is HandImageDisplayMode.AnimatedGif -> {
                    var animated by remember(mode.resourcePath) { mutableStateOf<AnimatedImage?>(null) }
                    LaunchedEffect(mode.resourcePath) {
                        animated = loadResourceAnimatedImage(mode.resourcePath)
                    }
                    val img = animated
                    if (img != null) {
                        Image(
                            bitmap = img.animate(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}
