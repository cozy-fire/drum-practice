package com.drumpractise.app.metronome

import androidx.compose.runtime.staticCompositionLocalOf

val LocalMetronomeEngine =
    staticCompositionLocalOf<MetronomeEngine> {
        error("LocalMetronomeEngine not provided — wrap content with CompositionLocalProvider in App")
    }
