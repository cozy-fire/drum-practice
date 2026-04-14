package com.drumpractise.app.separationpractice.generator

import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationItem
import com.drumpractise.app.separationpractice.model.SeparationPracticeLevel
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

object SeparationGenerator {
    /**
     * Generate items based on current config. Each selected tier N includes all
     * `separation_practice_N_*.musicxml` or `separation_practice_advance_N_*.musicxml` files
     * (whitelist; compose resources cannot list dirs).
     *
     * Random mode re-shuffles when caller updates [shuffleNonce] (e.g. on "确认设置").
     */
    fun generate(
        config: SeparationConfig,
        level: SeparationPracticeLevel,
        shuffleNonce: Int = 0,
    ): List<SeparationItem> {
        val allowed = config.points.filter { it in 1..4 }.toSet()
        if (allowed.isEmpty()) return emptyList()

        val map = if (level == SeparationPracticeLevel.Basic) filesByTier else advanceFilesByTier
        val paths =
            allowed
                .flatMap { tier -> map[tier].orEmpty() }
                .distinct()

        val baseItems =
            paths.map { path ->
                val tier = tierFromPath(path) ?: 1
                val suffix = path.substringAfterLast('_').removeSuffix(".musicxml")
                SeparationItem(
                    id = path.removePrefix("separation_practice/").removeSuffix(".musicxml"),
                    title = "$tier 个点位 · $suffix",
                    musicXmlPath = path,
                )
            }

        val ordered =
            when (config.mode) {
                SeparationPracticeMode.Sequential -> baseItems
                SeparationPracticeMode.Random -> baseItems.shuffled(kotlin.random.Random(shuffleNonce))
            }

        val loops = config.listLoopCount.coerceAtLeast(1)
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
        val name = path.substringAfterLast('/').removeSuffix(".musicxml")
        val prefix = "separation_practice_"
        if (!name.startsWith(prefix)) return null
        val rest = name.removePrefix(prefix)
        val segments = rest.split('_')
        return when (segments.firstOrNull()) {
            "advance" -> segments.getOrNull(1)?.toIntOrNull()
            else -> segments.firstOrNull()?.toIntOrNull()
        }
    }

    /** Paths relative to composeResources/files/ */
    private val filesByTier: Map<Int, List<String>> =
        mapOf(
            1 to
                listOf(
                    "separation_practice/separation_practice_1_1.musicxml",
                    "separation_practice/separation_practice_1_2.musicxml",
                    "separation_practice/separation_practice_1_3.musicxml",
                    "separation_practice/separation_practice_1_4.musicxml",
                ),
            2 to
                listOf(
                    "separation_practice/separation_practice_2_12.musicxml",
                    "separation_practice/separation_practice_2_23.musicxml",
                    "separation_practice/separation_practice_2_34.musicxml",
                    "separation_practice/separation_practice_2_14.musicxml",
                    "separation_practice/separation_practice_2_13.musicxml",
                    "separation_practice/separation_practice_2_24.musicxml",
                ),
            3 to
                listOf(
                    "separation_practice/separation_practice_3_123.musicxml",
                    "separation_practice/separation_practice_3_234.musicxml",
                    "separation_practice/separation_practice_3_134.musicxml",
                    "separation_practice/separation_practice_3_124.musicxml",
                ),
            4 to
                listOf(
                    "separation_practice/separation_practice_4_1234.musicxml",
                ),
        )

    private val advanceFilesByTier: Map<Int, List<String>> =
        mapOf(
            1 to
                listOf(
                    "separation_practice/separation_practice_advance_1_1.musicxml",
                    "separation_practice/separation_practice_advance_1_2.musicxml",
                    "separation_practice/separation_practice_advance_1_3.musicxml",
                    "separation_practice/separation_practice_advance_1_4.musicxml",
                ),
            2 to
                listOf(
                    "separation_practice/separation_practice_advance_2_12.musicxml",
                    "separation_practice/separation_practice_advance_2_23.musicxml",
                    "separation_practice/separation_practice_advance_2_34.musicxml",
                    "separation_practice/separation_practice_advance_2_14.musicxml",
                    "separation_practice/separation_practice_advance_2_13.musicxml",
                    "separation_practice/separation_practice_advance_2_24.musicxml",
                ),
            3 to
                listOf(
                    "separation_practice/separation_practice_advance_3_123.musicxml",
                    "separation_practice/separation_practice_advance_3_234.musicxml",
                    "separation_practice/separation_practice_advance_3_134.musicxml",
                    "separation_practice/separation_practice_advance_3_124.musicxml",
                ),
            4 to
                listOf(
                    "separation_practice/separation_practice_advance_4_1234.musicxml",
                ),
        )
}
