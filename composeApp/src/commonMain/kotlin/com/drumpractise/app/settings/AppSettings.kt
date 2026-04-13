package com.drumpractise.app.settings

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.accentshift.model.AccentShiftPracticeConfig
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.russhwolf.settings.Settings

object AppSettings {
    private const val KEY_STAFF_ZOOM_INDEX = "staff_zoom_index"
    private const val KEY_STAFF_ZOOM_CONFIGURED = "staff_zoom_configured"
    private const val KEY_STAFF_ZOOM_SCALE = "staff_zoom_scale"

    private const val KEY_SEPARATION_POINTS = "separation_points"
    private const val KEY_SEPARATION_CARD_LOOP_COUNT = "separation_card_loop_count"
    private const val KEY_SEPARATION_LIST_LOOP_COUNT = "separation_list_loop_count"
    private const val KEY_SEPARATION_BPM = "separation_bpm"
    private const val KEY_SEPARATION_MODE = "separation_mode"

    private const val KEY_ACCENT_SHIFT_POINTS = "accent_shift_points"
    private const val KEY_ACCENT_SHIFT_CARD_LOOP_COUNT = "accent_shift_card_loop_count"
    private const val KEY_ACCENT_SHIFT_LIST_LOOP_COUNT = "accent_shift_list_loop_count"
    private const val KEY_ACCENT_SHIFT_BPM = "accent_shift_bpm"
    private const val KEY_ACCENT_SHIFT_MODE = "accent_shift_mode"

    private val settings: Settings = Settings()

    fun getStaffZoomScale(): Float = settings.getFloatOrNull(KEY_STAFF_ZOOM_SCALE) ?: VerovioConfig.ZOOM_STEPS[0]

    fun setStaffZoomScale(scale: Float) {
        settings.putFloat(KEY_STAFF_ZOOM_SCALE, scale)
    }

    fun getStaffZoomConfigured(): Boolean = settings.getBoolean(KEY_STAFF_ZOOM_CONFIGURED, defaultValue = false)

    fun setStaffZoomConfigured(configured: Boolean) {
        settings.putBoolean(KEY_STAFF_ZOOM_CONFIGURED, configured)
    }

    fun getSeparationConfig(): SeparationConfig {
        val default = SeparationConfig.default()
        val pointsStr = settings.getStringOrNull(KEY_SEPARATION_POINTS)
        val points: Set<Int> =
            if (pointsStr.isNullOrBlank()) {
                default.points
            } else {
                pointsStr
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..4 }
                    .toSet()
                    .ifEmpty { default.points }
            }
        val cardLoopCount =
            settings.getIntOrNull(KEY_SEPARATION_CARD_LOOP_COUNT)?.coerceIn(1, 99)
                ?: default.cardLoopCount
        val listLoopCount =
            settings.getIntOrNull(KEY_SEPARATION_LIST_LOOP_COUNT)?.coerceIn(1, 99)
                ?: default.listLoopCount
        val bpm =
            settings.getIntOrNull(KEY_SEPARATION_BPM)?.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
                ?: default.bpm
        val mode =
            when (settings.getStringOrNull(KEY_SEPARATION_MODE)) {
                SeparationPracticeMode.Random.name -> SeparationPracticeMode.Random
                SeparationPracticeMode.Sequential.name -> SeparationPracticeMode.Sequential
                else -> default.mode
            }
        return SeparationConfig(
            points = points,
            cardLoopCount = cardLoopCount,
            listLoopCount = listLoopCount,
            bpm = bpm,
            mode = mode,
        )
    }

    fun setSeparationConfig(config: SeparationConfig) {
        val c = config.copy(
            points = config.points.filter { it in 1..4 }.toSet().ifEmpty { SeparationConfig.default().points },
            cardLoopCount = config.cardLoopCount.coerceIn(1, 99),
            listLoopCount = config.listLoopCount.coerceIn(1, 99),
            bpm = config.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
        )
        settings.putString(KEY_SEPARATION_POINTS, c.points.sorted().joinToString(","))
        settings.putInt(KEY_SEPARATION_CARD_LOOP_COUNT, c.cardLoopCount)
        settings.putInt(KEY_SEPARATION_LIST_LOOP_COUNT, c.listLoopCount)
        settings.putInt(KEY_SEPARATION_BPM, c.bpm)
        settings.putString(KEY_SEPARATION_MODE, c.mode.name)
    }

    fun getAccentShiftPracticeConfig(): AccentShiftPracticeConfig {
        val default = AccentShiftPracticeConfig.default()
        val pointsStr = settings.getStringOrNull(KEY_ACCENT_SHIFT_POINTS)
        val points: Set<Int> =
            if (pointsStr.isNullOrBlank()) {
                default.points
            } else {
                pointsStr
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..4 }
                    .toSet()
                    .ifEmpty { default.points }
            }
        val cardLoopCount =
            settings.getIntOrNull(KEY_ACCENT_SHIFT_CARD_LOOP_COUNT)?.coerceIn(1, 99)
                ?: default.cardLoopCount
        val listLoopCount =
            settings.getIntOrNull(KEY_ACCENT_SHIFT_LIST_LOOP_COUNT)?.coerceIn(1, 99)
                ?: default.listLoopCount
        val bpm =
            settings.getIntOrNull(KEY_ACCENT_SHIFT_BPM)?.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
                ?: default.bpm
        val mode =
            when (settings.getStringOrNull(KEY_ACCENT_SHIFT_MODE)) {
                SeparationPracticeMode.Random.name -> SeparationPracticeMode.Random
                SeparationPracticeMode.Sequential.name -> SeparationPracticeMode.Sequential
                else -> default.mode
            }
        return AccentShiftPracticeConfig(
            points = points,
            cardLoopCount = cardLoopCount,
            listLoopCount = listLoopCount,
            bpm = bpm,
            mode = mode,
        )
    }

    fun setAccentShiftPracticeConfig(config: AccentShiftPracticeConfig) {
        val c =
            config.copy(
                points = config.points.filter { it in 1..4 }.toSet().ifEmpty { AccentShiftPracticeConfig.default().points },
                cardLoopCount = config.cardLoopCount.coerceIn(1, 99),
                listLoopCount = config.listLoopCount.coerceIn(1, 99),
                bpm = config.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
            )
        settings.putString(KEY_ACCENT_SHIFT_POINTS, c.points.sorted().joinToString(","))
        settings.putInt(KEY_ACCENT_SHIFT_CARD_LOOP_COUNT, c.cardLoopCount)
        settings.putInt(KEY_ACCENT_SHIFT_LIST_LOOP_COUNT, c.listLoopCount)
        settings.putInt(KEY_ACCENT_SHIFT_BPM, c.bpm)
        settings.putString(KEY_ACCENT_SHIFT_MODE, c.mode.name)
    }
}

