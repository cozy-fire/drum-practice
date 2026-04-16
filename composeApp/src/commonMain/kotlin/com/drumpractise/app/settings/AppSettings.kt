package com.drumpractise.app.settings

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.accentshift.model.AccentShiftPracticeConfig
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeLevel
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.separationpractice.model.SeparationPracticeState
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

object AppSettings {
    private const val KEY_STAFF_ZOOM_INDEX = "staff_zoom_index"
    private const val KEY_STAFF_ZOOM_CONFIGURED = "staff_zoom_configured"
    private const val KEY_STAFF_ZOOM_SCALE = "staff_zoom_scale"

    private const val KEY_SEPARATION_BASIC_JSON = "separation_basic_json"
    private const val KEY_SEPARATION_ADVANCED_JSON = "separation_advanced_json"
    private const val KEY_SEPARATION_SELECTED_LEVEL = "separation_selected_level"

    /** Legacy keys (migrated into JSON once). */
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

    private const val KEY_METRONOME_BACKGROUND_PLAY_ENABLED = "metronome_background_play"
    private const val KEY_METRONOME_BACKGROUND_RUNNING = "metronome_background_running"

    private const val KEY_TABLET_WIDTH_BREAKPOINT_DP = "tablet_width_breakpoint_dp"

    private val settings: Settings = Settings()

    private val separationJson =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun getStaffZoomScale(): Float = settings.getFloatOrNull(KEY_STAFF_ZOOM_SCALE) ?: VerovioConfig.ZOOM_STEPS[0]

    fun setStaffZoomScale(scale: Float) {
        settings.putFloat(KEY_STAFF_ZOOM_SCALE, scale)
    }

    fun getStaffZoomConfigured(): Boolean = settings.getBoolean(KEY_STAFF_ZOOM_CONFIGURED, defaultValue = false)

    fun setStaffZoomConfigured(configured: Boolean) {
        settings.putBoolean(KEY_STAFF_ZOOM_CONFIGURED, configured)
    }

    fun getSeparationPracticeState(): SeparationPracticeState {
        val basicJson = settings.getStringOrNull(KEY_SEPARATION_BASIC_JSON)
        val advancedJson = settings.getStringOrNull(KEY_SEPARATION_ADVANCED_JSON)
        if (basicJson == null && advancedJson == null) {
            val migrated = migrateLegacySeparationConfigToState()
            if (migrated != null) {
                setSeparationPracticeState(migrated)
                clearLegacySeparationKeys()
                return migrated
            }
        }
        val basic =
            basicJson?.let {
                runCatching {
                    separationJson.decodeFromString(SeparationConfig.serializer(), it)
                }.getOrNull()
            }
                ?: SeparationConfig.default()
        val advanced =
            advancedJson?.let {
                runCatching {
                    separationJson.decodeFromString(SeparationConfig.serializer(), it)
                }.getOrNull()
            }
                ?: SeparationConfig.default()
        val level =
            when (settings.getStringOrNull(KEY_SEPARATION_SELECTED_LEVEL)) {
                SeparationPracticeLevel.Advanced.name -> SeparationPracticeLevel.Advanced
                else -> SeparationPracticeLevel.Basic
            }
        return SeparationPracticeState(
            selectedLevel = level,
            basicConfig = normalizeSeparationConfig(basic),
            advancedConfig = normalizeSeparationConfig(advanced),
        )
    }

    fun setSeparationPracticeState(state: SeparationPracticeState) {
        val s =
            SeparationPracticeState(
                selectedLevel = state.selectedLevel,
                basicConfig = normalizeSeparationConfig(state.basicConfig),
                advancedConfig = normalizeSeparationConfig(state.advancedConfig),
            )
        settings.putString(
            KEY_SEPARATION_BASIC_JSON,
            separationJson.encodeToString(SeparationConfig.serializer(), s.basicConfig),
        )
        settings.putString(
            KEY_SEPARATION_ADVANCED_JSON,
            separationJson.encodeToString(SeparationConfig.serializer(), s.advancedConfig),
        )
        settings.putString(KEY_SEPARATION_SELECTED_LEVEL, s.selectedLevel.name)
    }

    private fun migrateLegacySeparationConfigToState(): SeparationPracticeState? {
        val hasLegacy =
            settings.getStringOrNull(KEY_SEPARATION_POINTS) != null ||
                settings.getIntOrNull(KEY_SEPARATION_CARD_LOOP_COUNT) != null ||
                settings.getIntOrNull(KEY_SEPARATION_LIST_LOOP_COUNT) != null ||
                settings.getIntOrNull(KEY_SEPARATION_BPM) != null ||
                settings.getStringOrNull(KEY_SEPARATION_MODE) != null
        if (!hasLegacy) return null
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
        val basic =
            SeparationConfig(
                points = points,
                cardLoopCount = cardLoopCount,
                listLoopCount = listLoopCount,
                bpm = bpm,
                mode = mode,
            )
        return SeparationPracticeState(
            selectedLevel = SeparationPracticeLevel.Basic,
            basicConfig = normalizeSeparationConfig(basic),
            advancedConfig = SeparationConfig.default(),
        )
    }

    private fun clearLegacySeparationKeys() {
        settings.remove(KEY_SEPARATION_POINTS)
        settings.remove(KEY_SEPARATION_CARD_LOOP_COUNT)
        settings.remove(KEY_SEPARATION_LIST_LOOP_COUNT)
        settings.remove(KEY_SEPARATION_BPM)
        settings.remove(KEY_SEPARATION_MODE)
    }

    private fun normalizeSeparationConfig(config: SeparationConfig): SeparationConfig =
        config.copy(
            points = config.points.filter { it in 1..4 }.toSet().ifEmpty { SeparationConfig.default().points },
            cardLoopCount = config.cardLoopCount.coerceIn(1, 99),
            listLoopCount = config.listLoopCount.coerceIn(1, 99),
            bpm = config.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
        )

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

    fun getMetronomeBackgroundPlayEnabled(): Boolean =
        settings.getBoolean(KEY_METRONOME_BACKGROUND_PLAY_ENABLED, defaultValue = false)

    fun setMetronomeBackgroundPlayEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_METRONOME_BACKGROUND_PLAY_ENABLED, enabled)
    }

    fun getMetronomeBackgroundRunning(): Boolean =
        settings.getBoolean(KEY_METRONOME_BACKGROUND_RUNNING, defaultValue = false)

    fun setMetronomeBackgroundRunning(running: Boolean) {
        settings.putBoolean(KEY_METRONOME_BACKGROUND_RUNNING, running)
    }

    fun getTabletWidthBreakpointDp(): Float =
        settings.getFloatOrNull(KEY_TABLET_WIDTH_BREAKPOINT_DP) ?: 600f

    fun setTabletWidthBreakpointDp(breakpointDp: Float) {
        settings.putFloat(KEY_TABLET_WIDTH_BREAKPOINT_DP, breakpointDp)
    }
}
