package com.drumpractise.app.separationpractice.model

import com.drumpractise.app.separationpractice.model.SeparationPracticeMode.Random
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode.Sequential

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

enum class SeparationPracticeMode(val label: String) {
    Sequential("顺序练习"),
    Random("随机练习"),
}

