package com.drumpractise.app.separationpractice.generator

import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationItem
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

object SeparationGenerator {
    /**
     * Generate items based on current config. Each selected tier N includes all
     * `separation_practice_N_*.musicxml` files (whitelist; compose resources cannot list dirs).
     *
     * Random mode re-shuffles when caller updates [shuffleNonce] (e.g. on "确认设置").
     */
    fun generate(
        config: SeparationConfig,
        shuffleNonce: Int = 0,
    ): List<SeparationItem> {
        val allowed = config.points.filter { it in 1..4 }.toSet()
        if (allowed.isEmpty()) return emptyList()

        val paths =
            allowed
                .flatMap { tier -> filesByTier[tier].orEmpty() }
                .distinct()

        val items =
            paths.map { path ->
                val tier = tierFromPath(path) ?: 1
                val suffix = path.substringAfterLast('_').removeSuffix(".musicxml")
                SeparationItem(
                    id = path.removePrefix("separation_practice/").removeSuffix(".musicxml"),
                    title = "$tier 个点位 · $suffix",
                    musicXmlPath = path,
                )
            }

        return when (config.mode) {
            SeparationPracticeMode.Sequential -> items
            SeparationPracticeMode.Random -> items.shuffled(kotlin.random.Random(shuffleNonce))
        }
    }

    private fun tierFromPath(path: String): Int? {
        val name = path.substringAfterLast('/')
        val prefix = "separation_practice_"
        if (!name.startsWith(prefix)) return null
        val rest = name.removePrefix(prefix)
        return rest.substringBefore('_').toIntOrNull()
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
}
