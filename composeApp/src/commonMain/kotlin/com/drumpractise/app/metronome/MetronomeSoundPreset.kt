package com.drumpractise.app.metronome

/**
 * 节拍器音色预设。当前仅内置 [Tr707]；新增音色时在此增加枚举项，并补齐各平台资源与
 * `metronomeSampleBaseName`、`MetronomeEngine`（`rawResId` / `playBeep` / Bundle 加载）等映射。
 */
enum class MetronomeSoundPreset {
    Tr707,
}
