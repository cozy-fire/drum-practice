package com.drumpractise.app.separationpractice.model

import com.drumpractise.app.separationpractice.model.SeparationPracticeMode.Random
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode.Sequential
import kotlinx.serialization.Serializable

@Serializable
data class SeparationConfig(
    val points: Set<Int>,
    val cardLoopCount: Int,
    val listLoopCount: Int,
    val bpm: Int,
    val mode: SeparationPracticeMode,
) {
    companion object {
        fun default(): SeparationConfig =
            SeparationConfig(
                points = setOf(1, 2, 3, 4),
                cardLoopCount = 4,
                listLoopCount = 1,
                bpm = 110,
                mode = Sequential,
            )
    }
}

@Serializable
enum class SeparationPracticeMode(val label: String) {
    Sequential("顺序练习"),
    Random("随机练习"),
}

@Serializable
enum class SeparationPracticeLevel(val label: String) {
    Basic("基础"),
    Advanced("进阶"),
}

@Serializable
data class SeparationPracticeState(
    val selectedLevel: SeparationPracticeLevel,
    val basicConfig: SeparationConfig,
    val advancedConfig: SeparationConfig,
) {
    fun configForCurrentLevel(): SeparationConfig =
        when (selectedLevel) {
            SeparationPracticeLevel.Basic -> basicConfig
            SeparationPracticeLevel.Advanced -> advancedConfig
        }

    companion object {
        fun default(): SeparationPracticeState =
            SeparationPracticeState(
                selectedLevel = SeparationPracticeLevel.Basic,
                basicConfig = SeparationConfig.default(),
                advancedConfig = SeparationConfig.default(),
            )
    }
}
