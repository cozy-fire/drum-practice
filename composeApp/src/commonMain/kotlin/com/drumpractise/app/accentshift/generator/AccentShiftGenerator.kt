package com.drumpractise.app.accentshift.generator

import com.drumpractise.app.accentshift.model.AccentShiftItem
import com.drumpractise.app.accentshift.model.AccentShiftPracticeState
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

object AccentShiftGenerator {
    /**
     * 当前单选档位 N 包含所有 `accent_practice/accent_N_*.musicxml` 文件（白名单）。
     * Random mode re-shuffles when [shuffleNonce] changes (e.g. on confirm settings).
     */
    fun generate(
        state: AccentShiftPracticeState,
        shuffleNonce: Int = 0,
    ): List<AccentShiftItem> {
        val tier = state.selectedTier.coerceIn(1, 4)
        val params = state.paramsForTier(tier)
        val paths = filesByTier[tier].orEmpty()
        if (paths.isEmpty()) return emptyList()

        val baseItems =
            paths.map { path ->
                val tierFromFile = tierFromPath(path) ?: tier
                val suffix = path.substringAfterLast('_').removeSuffix(".musicxml")
                AccentShiftItem(
                    id = path.removePrefix("accent_practice/").removeSuffix(".musicxml"),
                    title = "$tierFromFile 个重音拍 · $suffix",
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
    private val filesByTier: Map<Int, List<String>> =
        mapOf(
            1 to
                listOf(
                    "accent_practice/accent_1_1.musicxml",
                    "accent_practice/accent_1_2.musicxml",
                    "accent_practice/accent_1_3.musicxml",
                    "accent_practice/accent_1_4.musicxml",
                ),
            2 to
                listOf(
                    "accent_practice/accent_2_12.musicxml",
                    "accent_practice/accent_2_13.musicxml",
                    "accent_practice/accent_2_14.musicxml",
                    "accent_practice/accent_2_23.musicxml",
                    "accent_practice/accent_2_24.musicxml",
                    "accent_practice/accent_2_34.musicxml",
                ),
            3 to
                listOf(
                    "accent_practice/accent_3_123.musicxml",
                    "accent_practice/accent_3_124.musicxml",
                    "accent_practice/accent_3_134.musicxml",
                    "accent_practice/accent_3_234.musicxml",
                ),
            4 to
                listOf(
                    "accent_practice/accent_4_1234.musicxml",
                ),
        )
}
