package com.drumpractise.app.separationpractice.util

import com.drumpractise.app.separationpractice.model.SeparationPracticeLevel
import drumhero.composeapp.generated.resources.Res
import drumhero.composeapp.generated.resources.separation_example_1_1
import drumhero.composeapp.generated.resources.separation_example_1_2
import drumhero.composeapp.generated.resources.separation_example_1_3
import drumhero.composeapp.generated.resources.separation_example_1_4
import drumhero.composeapp.generated.resources.separation_example_2_12
import drumhero.composeapp.generated.resources.separation_example_2_13
import drumhero.composeapp.generated.resources.separation_example_2_14
import drumhero.composeapp.generated.resources.separation_example_2_23
import drumhero.composeapp.generated.resources.separation_example_2_24
import drumhero.composeapp.generated.resources.separation_example_2_34
import drumhero.composeapp.generated.resources.separation_example_3_123
import drumhero.composeapp.generated.resources.separation_example_3_124
import drumhero.composeapp.generated.resources.separation_example_3_134
import drumhero.composeapp.generated.resources.separation_example_3_234
import drumhero.composeapp.generated.resources.separation_example_4_1234
import drumhero.composeapp.generated.resources.separation_practice_advance_example_1_1
import drumhero.composeapp.generated.resources.separation_practice_advance_example_1_2
import drumhero.composeapp.generated.resources.separation_practice_advance_example_1_3
import drumhero.composeapp.generated.resources.separation_practice_advance_example_1_4
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_12
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_13
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_14
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_23
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_24
import drumhero.composeapp.generated.resources.separation_practice_advance_example_2_34
import drumhero.composeapp.generated.resources.separation_practice_advance_example_3_123
import drumhero.composeapp.generated.resources.separation_practice_advance_example_3_124
import drumhero.composeapp.generated.resources.separation_practice_advance_example_3_134
import drumhero.composeapp.generated.resources.separation_practice_advance_example_3_234
import drumhero.composeapp.generated.resources.separation_practice_advance_example_4_1234
import org.jetbrains.compose.resources.DrawableResource

/** Tier → example drawables; keep in sync with SeparationGenerator MusicXML sets. Import each `separation_example_*` so generated accessors resolve (same pattern as divisorIcon). */
fun separationExampleImagesForTier(
    tier: Int,
    level: SeparationPracticeLevel,
): List<DrawableResource> =
    when (level) {
        SeparationPracticeLevel.Basic -> basicExampleImagesForTier(tier)
        SeparationPracticeLevel.Advanced -> advancedExampleImagesForTier(tier)
    }

private fun basicExampleImagesForTier(tier: Int): List<DrawableResource> =
    when (tier) {
        1 ->
            listOf(
                Res.drawable.separation_example_1_1,
                Res.drawable.separation_example_1_2,
                Res.drawable.separation_example_1_3,
                Res.drawable.separation_example_1_4,
            )
        2 ->
            listOf(
                Res.drawable.separation_example_2_12,
                Res.drawable.separation_example_2_23,
                Res.drawable.separation_example_2_34,
                Res.drawable.separation_example_2_14,
                Res.drawable.separation_example_2_13,
                Res.drawable.separation_example_2_24,
            )
        3 ->
            listOf(
                Res.drawable.separation_example_3_123,
                Res.drawable.separation_example_3_234,
                Res.drawable.separation_example_3_134,
                Res.drawable.separation_example_3_124,
            )
        4 ->
            listOf(
                Res.drawable.separation_example_4_1234,
            )
        else -> emptyList()
    }

/** 进阶模式示例图：`separation_practice_advance_example_*`，与 [basicExampleImagesForTier] 档位与顺序一致。 */
private fun advancedExampleImagesForTier(tier: Int): List<DrawableResource> =
    when (tier) {
        1 ->
            listOf(
                Res.drawable.separation_practice_advance_example_1_1,
                Res.drawable.separation_practice_advance_example_1_2,
                Res.drawable.separation_practice_advance_example_1_3,
                Res.drawable.separation_practice_advance_example_1_4,
            )
        2 ->
            listOf(
                Res.drawable.separation_practice_advance_example_2_12,
                Res.drawable.separation_practice_advance_example_2_23,
                Res.drawable.separation_practice_advance_example_2_34,
                Res.drawable.separation_practice_advance_example_2_14,
                Res.drawable.separation_practice_advance_example_2_13,
                Res.drawable.separation_practice_advance_example_2_24,
            )
        3 ->
            listOf(
                Res.drawable.separation_practice_advance_example_3_123,
                Res.drawable.separation_practice_advance_example_3_234,
                Res.drawable.separation_practice_advance_example_3_134,
                Res.drawable.separation_practice_advance_example_3_124,
            )
        4 ->
            listOf(
                Res.drawable.separation_practice_advance_example_4_1234,
            )
        else -> emptyList()
    }
