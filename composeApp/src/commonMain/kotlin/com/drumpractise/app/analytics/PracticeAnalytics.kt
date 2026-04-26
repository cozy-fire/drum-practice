package com.drumpractise.app.analytics

import com.drumpractise.app.accentshift.model.AccentShiftPracticeState
import com.drumpractise.app.data.DrumDatabaseSingleton
import com.drumpractise.app.platform.currentEpochMillis
import com.drumpractise.app.separationpractice.model.SeparationPracticeState
import com.drumpractise.app.settings.AppSettings
import kotlinx.serialization.json.Json

object PracticeKind {
    const val SEPARATION = "SEPARATION"
    const val ACCENT_SHIFT = "ACCENT_SHIFT"
}

object PracticeEventType {
    const val PRACTICE_ROUND_COMPLETED = "PRACTICE_ROUND_COMPLETED"
}

object PracticeAnalytics {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val repo by lazy { AnalyticsRepository(DrumDatabaseSingleton.instance) }

    fun recordSeparationPracticeRound(state: SeparationPracticeState) {
        val payloadJson = json.encodeToString(SeparationPracticeState.serializer(), state)
        insert(PracticeKind.SEPARATION, "SeparationPracticeState@v1", payloadJson)
    }

    fun recordAccentShiftPracticeRound(state: AccentShiftPracticeState) {
        val payloadJson = json.encodeToString(AccentShiftPracticeState.serializer(), state)
        insert(PracticeKind.ACCENT_SHIFT, "AccentShiftPracticeState@v1", payloadJson)
    }

    private fun insert(subject: String, payloadSchema: String, payloadJson: String) {
        repo.appendPracticeRoundCompleted(
            createdAt = currentEpochMillis(),
            localUserId = AppSettings.getOrCreateLocalUserId(),
            subject = subject,
            payloadSchema = payloadSchema,
            payloadJson = payloadJson,
        )
    }
}
