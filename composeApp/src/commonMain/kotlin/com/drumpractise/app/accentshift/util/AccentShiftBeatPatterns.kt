package com.drumpractise.app.accentshift.util

/** Beat indices (1..4) accented in each pattern for tier [tier], same order as [AccentShiftGenerator] files. */
fun accentBeatPatternsForTier(tier: Int): List<List<Int>> =
    when (tier) {
        1 -> listOf(listOf(1), listOf(2), listOf(3), listOf(4))
        2 ->
            listOf(
                listOf(1, 2),
                listOf(1, 3),
                listOf(1, 4),
                listOf(2, 3),
                listOf(2, 4),
                listOf(3, 4),
            )
        3 ->
            listOf(
                listOf(1, 2, 3),
                listOf(1, 2, 4),
                listOf(1, 3, 4),
                listOf(2, 3, 4),
            )
        4 -> listOf(listOf(1, 2, 3, 4))
        else -> emptyList()
    }
