package com.drumpractise.app.accentshift.model

import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import kotlinx.serialization.Serializable

@Serializable
data class AccentShiftPracticeConfig(
    /** Enabled tiers 1..4: how many beats in the bar carry an accent. */
    val points: Set<Int>,
    val cardLoopCount: Int,
    val listLoopCount: Int,
    val bpm: Int,
    val mode: SeparationPracticeMode,
) {
    companion object {
        fun default(): AccentShiftPracticeConfig =
            AccentShiftPracticeConfig(
                points = setOf(1, 2, 3, 4),
                cardLoopCount = 4,
                listLoopCount = 1,
                bpm = 110,
                mode = SeparationPracticeMode.Sequential,
            )
    }
}
