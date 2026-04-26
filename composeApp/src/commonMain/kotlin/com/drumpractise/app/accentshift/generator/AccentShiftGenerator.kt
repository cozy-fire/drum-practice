package com.drumpractise.app.accentshift.generator

import com.drumpractise.app.accentshift.model.AccentShiftItem
import com.drumpractise.app.accentshift.model.AccentShiftPracticeState
import com.drumpractise.app.accentshift.model.AccentShiftTierSelection
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

object AccentShiftGenerator {
    /**
     * 当前点位包含所有 `accent_practice/accent_N_*.musicxml` 文件（白名单）。
     * Random mode re-shuffles when [shuffleNonce] changes (e.g. on confirm settings).
     */
    fun generate(
        state: AccentShiftPracticeState,
        shuffleNonce: Int = 0,
    ): List<AccentShiftItem> {
        val selection = state.selectedTier
        val params = state.currentParams()
        val paths = filesBySelection[selection].orEmpty()
        if (paths.isEmpty()) return emptyList()

        val baseItems =
            paths.map { path ->
                val tierFromFile = tierFromPath(path)
                val suffix = path.substringAfterLast('_').removeSuffix(".musicxml")
                AccentShiftItem(
                    id = path.removePrefix("accent_practice/").removeSuffix(".musicxml"),
                    title = "${tierFromFile ?: 1} 个重音拍 · $suffix",
                    musicXmlPath = path,
                )
            }

        val ordered =
            when (params.mode) {
                SeparationPracticeMode.Sequential -> baseItems
                SeparationPracticeMode.Random -> baseItems.shuffled(kotlin.random.Random(shuffleNonce))
            }

        val loops = params.listLoopCount.coerceAtLeast(1)
        if (loops == 1) return ordered

        return buildList(capacity = ordered.size * loops) {
            for (round in 1..loops) {
                for (base in ordered) {
                    add(
                        base.copy(
                            id = "${base.id}#r$round",
                            title = "${base.title}（第 $round/$loops 轮）",
                        ),
                    )
                }
            }
        }
    }

    private fun tierFromPath(path: String): Int? {
        val name = path.substringAfterLast('/')
        if (!name.startsWith("accent_")) return null
        val rest = name.removePrefix("accent_")
        return rest.substringBefore('_').toIntOrNull()
    }

    /** Paths relative to composeResources/files/ */
    private val tier1Files =
        listOf(
            "accent_practice/accent_1_1.musicxml",
            "accent_practice/accent_1_2.musicxml",
            "accent_practice/accent_1_3.musicxml",
            "accent_practice/accent_1_4.musicxml",
        )

    private val tier2Files =
        listOf(
            "accent_practice/accent_2_12.musicxml",
            "accent_practice/accent_2_13.musicxml",
            "accent_practice/accent_2_14.musicxml",
            "accent_practice/accent_2_23.musicxml",
            "accent_practice/accent_2_24.musicxml",
            "accent_practice/accent_2_34.musicxml",
        )

    private val filesBySelection: Map<AccentShiftTierSelection, List<String>> =
        mapOf(
            AccentShiftTierSelection.Tier1 to tier1Files,
            AccentShiftTierSelection.Tier2 to tier2Files,
            // Combo: mixed pool; Sequential will follow list order.
            AccentShiftTierSelection.Combo12 to (tier1Files + tier2Files),
        )
}
