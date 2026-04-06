package com.drumpractise.app.metronome

/**
 * 与磁盘资源一致的逻辑名（无扩展名）。
 * Android：`res/raw/{name}.mp3` / `.wav` 等（小写+下划线）。
 * iOS：Bundle 内 `{name}.wav`（与 sync 任务目标路径一致）。
 *
 * 当前仅 [MetronomeSoundPreset.Tr707]；**Tr707** 仅两种物理采样（strong / weak），
 * **次强（Medium）与弱音共用 `tr707_weak`**，故文件名上不会出现 `tr707_medium`。
 */
internal fun metronomePresetResourcePrefix(preset: MetronomeSoundPreset): String =
    when (preset) {
        MetronomeSoundPreset.Tr707 -> "tr707"
    }

internal fun metronomeSampleBaseName(
    preset: MetronomeSoundPreset,
    tier: MetronomeAccent,
): String {
    val p = metronomePresetResourcePrefix(preset)
    val effectiveTier =
        if (preset == MetronomeSoundPreset.Tr707 && tier == MetronomeAccent.Medium) {
            MetronomeAccent.Weak
        } else {
            tier
        }
    val t =
        when (effectiveTier) {
            MetronomeAccent.Strong -> "strong"
            MetronomeAccent.Medium -> "medium"
            MetronomeAccent.Weak -> "weak"
        }
    return "${p}_$t"
}
