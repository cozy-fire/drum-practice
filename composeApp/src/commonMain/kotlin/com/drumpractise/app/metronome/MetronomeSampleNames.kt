package com.drumpractise.app.metronome

/**
 * 与磁盘资源一致的逻辑名（无扩展名）。
 * Android：`res/raw/{name}.wav`（小写+下划线）。
 * iOS：Bundle 内 `{name}.wav`（与 sync 任务目标路径一致）。
 *
 * 共 8 种 [MetronomeSoundPreset] × 3 档 [MetronomeAccent] = 24 个文件。
 */
internal fun metronomeSampleBaseName(
    preset: MetronomeSoundPreset,
    tier: MetronomeAccent,
): String {
    val p =
        when (preset) {
            MetronomeSoundPreset.ClickWood -> "clickwood"
            MetronomeSoundPreset.BeepHigh -> "beephigh"
            MetronomeSoundPreset.BeepLow -> "beeplow"
            MetronomeSoundPreset.Digital -> "digital"
            MetronomeSoundPreset.Bell -> "bell"
            MetronomeSoundPreset.SharpClick -> "sharpclick"
            MetronomeSoundPreset.WoodKnock -> "woodknock"
            MetronomeSoundPreset.SoftTick -> "softtick"
        }
    val t =
        when (tier) {
            MetronomeAccent.Strong -> "strong"
            MetronomeAccent.Medium -> "medium"
            MetronomeAccent.Weak -> "weak"
        }
    return "${p}_$t"
}
