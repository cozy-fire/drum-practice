package com.drumpractise.app.separationpractice.generator

import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationItem
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode

object SeparationGenerator {
    /**
     * Generate items based on current config.
     *
     * Note: random mode should only re-shuffle when caller updates [shuffleNonce]
     * (e.g. on "确认设置"). This prevents UI jitter during draft edits.
     */
    fun generate(
        config: SeparationConfig,
        shuffleNonce: Int = 0,
    ): List<SeparationItem> {
        val allowed = config.points.filter { it in 1..4 }.toSet()
        if (allowed.isEmpty()) return emptyList()

        val combos = allKnownCombos().filter { it.points.all { p -> p in allowed } }
        val ordered =
            combos.sortedWith(
                compareBy<Combo>({ it.points.size }, { it.points.joinToString("") }),
            )

        val list =
            ordered.map { c ->
                val id = c.points.joinToString("")
                val title = when (c.points.size) {
                    1 -> "点位 ${c.points.single()}"
                    else -> "点位 ${c.points.joinToString(" + ")}"
                }
                SeparationItem(
                    id = id,
                    title = title,
                    musicXmlPath = c.musicXmlPath,
                )
            }

        return when (config.mode) {
            SeparationPracticeMode.Sequential -> list
            SeparationPracticeMode.Random -> list.shuffled(kotlin.random.Random(shuffleNonce))
        }
    }

    private data class Combo(
        val points: List<Int>,
        val musicXmlPath: String,
    )

    private fun allKnownCombos(): List<Combo> {
        // Paths are relative to composeResources/files/
        fun combo(points: List<Int>): Combo {
            val sorted = points.sorted()
            val key = sorted.joinToString("")
            val path =
                "separation_practice/separation_practice_${sorted.size}_$key.musicxml"
            return Combo(points = sorted, musicXmlPath = path)
        }

        return buildList {
            addAll((1..4).map { combo(listOf(it)) })
            addAll(
                listOf(
                    combo(listOf(1, 2)),
                    combo(listOf(2, 3)),
                    combo(listOf(3, 4)),
                    combo(listOf(1, 4)),
                    combo(listOf(1, 3)),
                    combo(listOf(2, 4)),
                ),
            )
            addAll(
                listOf(
                    combo(listOf(1, 2, 3)),
                    combo(listOf(2, 3, 4)),
                    combo(listOf(1, 3, 4)),
                    combo(listOf(1, 2, 4)),
                ),
            )
            add(combo(listOf(1, 2, 3, 4)))
        }
    }
}

