package com.drumpractise.app.score

import androidx.compose.runtime.Composable

@Composable
actual fun rememberScoreHitSoundPlayer(): ScoreHitSoundPlayer = NoOpScoreHitSoundPlayer
