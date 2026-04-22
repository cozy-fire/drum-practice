package com.drumpractise.app.analytics

import com.drumpractise.app.accentshift.model.AccentShiftPracticeConfig
import com.drumpractise.app.data.openDrumDatabase
import com.drumpractise.app.platform.currentEpochMillis
import com.drumpractise.app.separationpractice.model.SeparationPracticeState
import com.drumpractise.app.settings.AppSettings
import kotlinx.serialization.json.Json

object PracticeKind {
    const val SEPARATION = "SEPARATION"
    const val ACCENT_SHIFT = "ACCENT_SHIFT"
}

object PracticeAnalytics {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val db by lazy { openDrumDatabase() }

    fun recordSeparationPracticeRound(state: SeparationPracticeState) {
        val settingsJson = json.encodeToString(SeparationPracticeState.serializer(), state)
        insert(PracticeKind.SEPARATION, settingsJson)
    }

    fun recordAccentShiftPracticeRound(config: AccentShiftPracticeConfig) {
        val settingsJson = json.encodeToString(AccentShiftPracticeConfig.serializer(), config)
        insert(PracticeKind.ACCENT_SHIFT, settingsJson)
    }

    private fun insert(practiceKind: String, settingsJson: String) {
        val userId = AppSettings.getOrCreateLocalUserId()
        db.practiceEventQueries.insertPracticeEvent(
            created_at = currentEpochMillis(),
            local_user_id = userId,
            practice_kind = practiceKind,
            settings_json = settingsJson,
        )
    }
}
