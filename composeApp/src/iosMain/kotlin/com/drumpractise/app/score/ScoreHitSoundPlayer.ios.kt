package com.drumpractise.app.score

import androidx.compose.runtime.Composable

actual fun globalScoreHitSoundPlayer(): ScoreHitSoundPlayer = NoOpScoreHitSoundPlayer

@Composable
actual fun rememberScoreHitSoundPlayer(): ScoreHitSoundPlayer = NoOpScoreHitSoundPlayer
