package com.drumpractise.app.constance

object VerovioConfig {
    /** StaffPreview 的缩放步进（来源：原 MusicXmlScoreScreenContent/SettingsScreen 的 zoomSteps）。 */
    val ZOOM_STEPS: List<Float> = List(14) { i -> 0.85f + 0.15f * i }

    /** 谱面击打混音：未单独配置的乐器 id 使用的默认音量（与原先 Android 统一 HIT_VOLUME 对齐）。 */
    const val DEFAULT_INSTRUMENT_VOLUME = 0.9f

    /** 底鼓（MusicXML `P1-I36`）击打相对音量。 */
    const val DRUM_KICK_HIT_VOLUME = 1.1f

    /** 闭踩镲（MusicXML `P1-I42`）击打相对音量。 */
    const val DRUM_CLOSED_HI_HAT_HIT_VOLUME = 0.4f
}

/** 按 MusicXML `score-instrument` id 返回谱面击打混音音量（与 [com.drumpractise.app.score] 内 `ActiveVoice.vol` 语义一致）。 */
fun hitVolumeForInstrument(instrumentId: String): Float =
    when (instrumentId) {
        "P1-I36" -> VerovioConfig.DRUM_KICK_HIT_VOLUME
        "P1-I42" -> VerovioConfig.DRUM_CLOSED_HI_HAT_HIT_VOLUME
        else -> VerovioConfig.DEFAULT_INSTRUMENT_VOLUME
    }