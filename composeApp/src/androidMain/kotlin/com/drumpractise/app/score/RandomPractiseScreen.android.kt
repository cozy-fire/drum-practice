package com.drumpractise.app.score

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@Composable
actual fun RandomPractiseScreen(onBack: () -> Unit) {
    RandomPractiseScreenContent(onBack = onBack, modifier = Modifier.fillMaxSize())
}
