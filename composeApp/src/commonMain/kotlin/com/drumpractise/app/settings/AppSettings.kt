package com.drumpractise.app.settings

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.accentshift.model.AccentShiftPracticeState
import com.drumpractise.app.metronome.MetronomePracticeItem
import com.drumpractise.app.metronome.MetronomePracticePersistState
import com.drumpractise.app.metronome.normalized
import com.drumpractise.app.metronome.normalizeNoteDivisor
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeLevel
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.separationpractice.model.SeparationPracticeState
import com.russhwolf.settings.Settings
import kotlin.random.Random
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

    private const val KEY_ACCENT_SHIFT_STATE_JSON = "accent_shift_state_json"

    private const val KEY_METRONOME_BACKGROUND_PLAY_ENABLED = "metronome_background_play"
    private const val KEY_METRONOME_BACKGROUND_RUNNING = "metronome_background_running"
    private const val KEY_METRONOME_PRACTICE_STATE_JSON = "metronome_practice_state_json"

    private const val KEY_TABLET_WIDTH_BREAKPOINT_DP = "tablet_width_breakpoint_dp"

    private const val KEY_LOCAL_USER_ID = "local_user_id"

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

    fun getAccentShiftPracticeState(): AccentShiftPracticeState {
        val raw = settings.getStringOrNull(KEY_ACCENT_SHIFT_STATE_JSON) ?: return AccentShiftPracticeState.default().normalized()
        return runCatching {
            separationJson.decodeFromString(AccentShiftPracticeState.serializer(), raw)
        }.getOrNull()?.normalized() ?: AccentShiftPracticeState.default().normalized()
    }

    fun setAccentShiftPracticeState(state: AccentShiftPracticeState) {
        val normalized = state.normalized()
        settings.putString(
            KEY_ACCENT_SHIFT_STATE_JSON,
            separationJson.encodeToString(AccentShiftPracticeState.serializer(), normalized),
        )
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

    private fun loadMetronomePracticePersistState(): MetronomePracticePersistState {
        val raw = settings.getStringOrNull(KEY_METRONOME_PRACTICE_STATE_JSON) ?: return MetronomePracticePersistState.default()
        return runCatching {
            separationJson.decodeFromString(MetronomePracticePersistState.serializer(), raw)
        }.getOrNull()?.normalized() ?: MetronomePracticePersistState.default()
    }

    private fun saveMetronomePracticePersistState(state: MetronomePracticePersistState) {
        val normalized = state.normalized()
        settings.putString(
            KEY_METRONOME_PRACTICE_STATE_JSON,
            separationJson.encodeToString(MetronomePracticePersistState.serializer(), normalized),
        )
    }

    fun getMetronomePracticeItemBpm(item: MetronomePracticeItem): Int {
        val s = loadMetronomePracticePersistState()
        return when (item) {
            MetronomePracticeItem.Single -> s.single.bpm
            MetronomePracticeItem.Double -> s.doubleStroke.bpm
        }
    }

    fun setMetronomePracticeItemBpm(item: MetronomePracticeItem, bpm: Int) {
        val coerced = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
        val current = loadMetronomePracticePersistState()
        val updated =
            when (item) {
                MetronomePracticeItem.Single -> current.copy(single = current.single.copy(bpm = coerced))
                MetronomePracticeItem.Double -> current.copy(doubleStroke = current.doubleStroke.copy(bpm = coerced))
            }
        saveMetronomePracticePersistState(updated)
    }

    fun getMetronomePracticeItemNoteDivisor(item: MetronomePracticeItem): Int {
        val s = loadMetronomePracticePersistState()
        val raw =
            when (item) {
                MetronomePracticeItem.Single -> s.single.noteDivisor
                MetronomePracticeItem.Double -> s.doubleStroke.noteDivisor
            }
        return normalizeNoteDivisor(raw)
    }

    fun setMetronomePracticeItemNoteDivisor(item: MetronomePracticeItem, divisor: Int) {
        val d = normalizeNoteDivisor(divisor)
        val current = loadMetronomePracticePersistState()
        val updated =
            when (item) {
                MetronomePracticeItem.Single -> current.copy(single = current.single.copy(noteDivisor = d))
                MetronomePracticeItem.Double -> current.copy(doubleStroke = current.doubleStroke.copy(noteDivisor = d))
            }
        saveMetronomePracticePersistState(updated)
    }

    fun getOrCreateLocalUserId(): String {
        val existing = settings.getStringOrNull(KEY_LOCAL_USER_ID)?.trim().orEmpty()
        if (existing.isNotEmpty()) return existing
        val hex = "0123456789abcdef"
        val newId =
            buildString(32) {
                repeat(32) {
                    append(hex[Random.nextInt(hex.length)])
                }
            }
        settings.putString(KEY_LOCAL_USER_ID, newId)
        return newId
    }

    fun getTabletWidthBreakpointDp(): Float =
        settings.getFloatOrNull(KEY_TABLET_WIDTH_BREAKPOINT_DP) ?: 600f

    fun setTabletWidthBreakpointDp(breakpointDp: Float) {
        settings.putFloat(KEY_TABLET_WIDTH_BREAKPOINT_DP, breakpointDp)
    }
}
