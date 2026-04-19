package com.drumpractise.app.practice

import com.drumpractise.app.constance.MetronomeConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

actual suspend fun playPracticeCountIn(
    bpm: Int,
    onBeat: ((Int) -> Unit)?,
) {
    val bpmClamped = bpm.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
    val msPerQuarter = (60000.0 / bpmClamped).toLong().coerceAtLeast(1L)
    for (beat in 1..4) {
        withContext(Dispatchers.Main) {
            onBeat?.invoke(beat)
        }
        delay(msPerQuarter)
    }
}
