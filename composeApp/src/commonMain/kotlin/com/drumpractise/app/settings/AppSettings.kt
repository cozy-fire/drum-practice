package com.drumpractise.app.settings

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.russhwolf.settings.Settings

object AppSettings {
    private const val KEY_STAFF_ZOOM_INDEX = "staff_zoom_index"
    private const val KEY_STAFF_ZOOM_CONFIGURED = "staff_zoom_configured"
    private const val KEY_STAFF_ZOOM_SCALE = "staff_zoom_scale"

    private const val KEY_SEPARATION_POINTS = "separation_points"
    private const val KEY_SEPARATION_LOOP_COUNT = "separation_loop_count"
    private const val KEY_SEPARATION_BPM = "separation_bpm"
    private const val KEY_SEPARATION_MODE = "separation_mode"

    private val settings: Settings = Settings()

    fun getStaffZoomIndex(): Int? = settings.getIntOrNull(KEY_STAFF_ZOOM_INDEX)

    fun setStaffZoomIndex(index: Int) {
        settings.putInt(KEY_STAFF_ZOOM_INDEX, index)
    }

    fun getStaffZoomScale(): Float {
        settings.getFloatOrNull(KEY_STAFF_ZOOM_SCALE)?.let { return it }

        // 兼容迁移：旧版本仅保存 index。
        val index = getStaffZoomIndex()
        val fallback =
            if (index == null) {
                VerovioConfig.ZOOM_STEPS.getOrElse(2) { 0.85f }
            } else {
                VerovioConfig.ZOOM_STEPS.getOrElse(index) { VerovioConfig.ZOOM_STEPS.getOrElse(2) { 0.85f } }
            }
        settings.putFloat(KEY_STAFF_ZOOM_SCALE, fallback)
        return fallback
    }

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
        val loop =
            settings.getIntOrNull(KEY_SEPARATION_LOOP_COUNT)?.coerceIn(1, 99)
                ?: default.loopCount
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
            loopCount = loop,
            bpm = bpm,
            mode = mode,
        )
    }

    fun setSeparationConfig(config: SeparationConfig) {
        val c = config.copy(
            points = config.points.filter { it in 1..4 }.toSet().ifEmpty { SeparationConfig.default().points },
            loopCount = config.loopCount.coerceIn(1, 99),
            bpm = config.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
        )
        settings.putString(KEY_SEPARATION_POINTS, c.points.sorted().joinToString(","))
        settings.putInt(KEY_SEPARATION_LOOP_COUNT, c.loopCount)
        settings.putInt(KEY_SEPARATION_BPM, c.bpm)
        settings.putString(KEY_SEPARATION_MODE, c.mode.name)
    }
}

