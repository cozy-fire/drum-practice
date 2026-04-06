package com.drumpractise.app.score

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@Composable
actual fun MusicXmlScoreScreen(onBack: () -> Unit) {
    MusicXmlScoreScreenContent(onBack = onBack, modifier = Modifier.fillMaxSize())
}
