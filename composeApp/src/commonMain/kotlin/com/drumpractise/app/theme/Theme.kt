package com.drumpractise.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Lavender = Color(0xFFD0BCFF)
private val LavenderDim = Color(0xFF9A82DB)
private val IceBlue = Color(0xFF8EC5FF)
private val DeepBg = Color(0xFF121018)
private val SurfaceDark = Color(0xFF1E1A24)
private val SurfaceVariantDark = Color(0xFF2A2533)
private val OutlineMuted = Color(0xFF6B5B8C)
private val AccentBeat = Color(0xFFFF8A95)

val DrumAccentBeat = AccentBeat

private val DrumDarkScheme =
    darkColorScheme(
        primary = Lavender,
        onPrimary = Color(0xFF1A1028),
        primaryContainer = LavenderDim,
        onPrimaryContainer = Color(0xFFE8DDFF),
        secondary = IceBlue,
        onSecondary = Color(0xFF001E2F),
        secondaryContainer = Color(0xFF3A4A6B),
        onSecondaryContainer = Color(0xFFD6E3FF),
        tertiary = AccentBeat,
        onTertiary = Color(0xFF3E0A12),
        background = DeepBg,
        onBackground = Color(0xFFE8E0F0),
        surface = SurfaceDark,
        onSurface = Color(0xFFE8E0F0),
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = Color(0xFFCBC4DC),
        outline = OutlineMuted,
        outlineVariant = Color(0xFF4A3F5C),
    )

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DrumDarkScheme,
        content = content,
    )
}
