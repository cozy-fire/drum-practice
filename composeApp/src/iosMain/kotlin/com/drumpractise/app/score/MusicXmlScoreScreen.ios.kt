package com.drumpractise.app.score

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun MusicXmlScoreScreen(onBack: () -> Unit) {
    MusicXmlScoreScreenContent(onBack = onBack, modifier = Modifier.fillMaxSize())
}
