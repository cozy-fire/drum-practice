package com.drumpractise.app.accentshift.model

import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import kotlinx.serialization.Serializable

@Serializable
data class AccentShiftTierParams(
    val cardLoopCount: Int,
    val listLoopCount: Int,
    val bpm: Int,
    val mode: SeparationPracticeMode,
) {
    companion object {
        fun default(): AccentShiftTierParams =
            AccentShiftTierParams(
                cardLoopCount = 4,
                listLoopCount = 1,
                bpm = 110,
                mode = SeparationPracticeMode.Sequential,
            )
    }
}

/**
 * 重音移位练习持久化状态：单选档位 1..4，每档独立 BPM / 循环 / 模式。
 */
@Serializable
data class AccentShiftPracticeState(
    val selectedTier: Int,
    val tiers: List<AccentShiftTierParams>,
) {
    fun normalized(): AccentShiftPracticeState {
        val base =
            if (tiers.size == 4) {
                tiers
            } else {
                List(4) { AccentShiftTierParams.default() }
            }
        return copy(
            selectedTier = selectedTier.coerceIn(1, 4),
            tiers =
                base.map { t ->
                    t.copy(
                        cardLoopCount = t.cardLoopCount.coerceIn(1, 99),
                        listLoopCount = t.listLoopCount.coerceIn(1, 99),
                        bpm = t.bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX),
                    )
                },
        )
    }

    fun paramsForTier(tier: Int): AccentShiftTierParams =
        tiers.getOrNull(tier.coerceIn(1, 4) - 1) ?: AccentShiftTierParams.default()

    fun currentParams(): AccentShiftTierParams = paramsForTier(selectedTier)

    fun selectTier(tier: Int): AccentShiftPracticeState = copy(selectedTier = tier.coerceIn(1, 4))

    fun updateCurrentTier(transform: (AccentShiftTierParams) -> AccentShiftTierParams): AccentShiftPracticeState {
        val idx = selectedTier.coerceIn(1, 4) - 1
        val next = tiers.toMutableList()
        next[idx] = transform(next[idx])
        return copy(tiers = next).normalized()
    }

    companion object {
        fun default(): AccentShiftPracticeState =
            AccentShiftPracticeState(
                selectedTier = 1,
                tiers = List(4) { AccentShiftTierParams.default() },
            )
    }
}
