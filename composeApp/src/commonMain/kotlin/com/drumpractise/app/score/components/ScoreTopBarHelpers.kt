package com.drumpractise.app.score.components

import drum_practice.composeapp.generated.resources.Res
import drum_practice.composeapp.generated.resources.metronome_note_four_sixteenth_unselected
import drum_practice.composeapp.generated.resources.metronome_note_quarter_unselected
import drum_practice.composeapp.generated.resources.metronome_note_two_eighth_unselected
import org.jetbrains.compose.resources.DrawableResource

internal fun divisorLabel(noteDivisor: Int): String =
    when (noteDivisor) {
        1 -> "1/4"
        2 -> "1/8"
        4 -> "1/16"
        else -> "1/4"
    }

internal fun divisorIcon(noteDivisor: Int): DrawableResource =
    when (noteDivisor) {
        1 -> Res.drawable.metronome_note_quarter_unselected
        2 -> Res.drawable.metronome_note_two_eighth_unselected
        4 -> Res.drawable.metronome_note_four_sixteenth_unselected
        else -> Res.drawable.metronome_note_quarter_unselected
    }
