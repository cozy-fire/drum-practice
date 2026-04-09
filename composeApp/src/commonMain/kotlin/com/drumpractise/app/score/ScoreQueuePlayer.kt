package com.drumpractise.app.score

import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.musicxml.MusicXmlRepository
import com.drumpractise.app.score.musicxml.buildDrumScorePlaybackSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class MusicXmlQueueItem(
    val id: String,
    /** Relative to composeResources/files/ */
    val musicXmlPath: String,
)

sealed interface ScoreQueueState {
    data object Idle : ScoreQueueState

    data class Playing(
        val index: Int,
        val total: Int,
        val itemId: String,
    ) : ScoreQueueState

    data object Ended : ScoreQueueState

    data object Stopped : ScoreQueueState

    data class Error(
        val message: String,
    ) : ScoreQueueState
}

sealed interface ScoreQueueEvent {
    data class ItemStarted(val index: Int, val itemId: String) : ScoreQueueEvent

    data class ItemCompleted(val index: Int, val itemId: String) : ScoreQueueEvent

    data object QueueCompleted : ScoreQueueEvent
}

class ScorePlaybackSession internal constructor(
    val state: StateFlow<ScoreQueueState>,
    val events: SharedFlow<ScoreQueueEvent>,
    private val stopImpl: () -> Unit,
) {
    fun stop() = stopImpl()
}

class ScoreQueuePlayer(
    private val hitSoundPlayer: ScoreHitSoundPlayer,
) {
    fun playQueue(
        items: List<MusicXmlQueueItem>,
        bpm: Int,
        loopCount: Int,
        pcmSampleRate: Int = 48_000,
        /** Inclusive index in [items] to begin playback (e.g. resume after pause). */
        startIndex: Int = 0,
    ): ScorePlaybackSession {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val state = MutableStateFlow<ScoreQueueState>(ScoreQueueState.Idle)
        val events = MutableSharedFlow<ScoreQueueEvent>(extraBufferCapacity = 16)

        var job: Job? = null

        fun stop(reasonState: ScoreQueueState = ScoreQueueState.Stopped) {
            job?.cancel()
            job = null
            hitSoundPlayer.stopPlayback()
            state.value = reasonState
            scope.cancel()
        }

        job =
            scope.launch {
                val loops = loopCount.coerceAtLeast(1)
                val from =
                    if (items.isEmpty()) {
                        0
                    } else {
                        startIndex.coerceIn(0, items.lastIndex)
                    }
                try {
                    for (idx in from until items.size) {
                        val item = items[idx]
                        state.value = ScoreQueueState.Playing(index = idx, total = items.size, itemId = item.id)
                        events.tryEmit(ScoreQueueEvent.ItemStarted(index = idx, itemId = item.id))

                        val xml = MusicXmlRepository.getXml(item.musicXmlPath)
                        if (xml.isBlank()) {
                            events.tryEmit(ScoreQueueEvent.ItemCompleted(index = idx, itemId = item.id))
                            continue
                        }

                        if (hitSoundPlayer.supportsFiniteCompletion && hitSoundPlayer !== NoOpScoreHitSoundPlayer) {
                            suspendCancellableCoroutine { cont ->
                                cont.invokeOnCancellation { hitSoundPlayer.stopPlayback() }
                                hitSoundPlayer.startPlaybackFinite(
                                    musicXml = xml,
                                    bpm = bpm,
                                    loopCount = loops,
                                    onCompleted = { cont.resume(Unit) },
                                )
                            }
                        } else {
                            // Fallback for non-Android targets: estimate duration then delay.
                            val schedule =
                                buildDrumScorePlaybackSchedule(
                                    parsed = MusicXmlDrumTimelineParser.parse(xml),
                                    bpm = bpm,
                                    pcmSampleRate = pcmSampleRate,
                                )
                            if (schedule != null) {
                                val loopMs =
                                    (schedule.loopLengthSamples / pcmSampleRate.toDouble() * 1000.0)
                                        .coerceAtLeast(1.0)
                                hitSoundPlayer.startPlayback(xml, bpm)
                                delay((loopMs * loops).toLong())
                            }
                        }

                        events.tryEmit(ScoreQueueEvent.ItemCompleted(index = idx, itemId = item.id))
                    }

                    hitSoundPlayer.stopPlayback()
                    state.value = ScoreQueueState.Ended
                    events.tryEmit(ScoreQueueEvent.QueueCompleted)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    hitSoundPlayer.stopPlayback()
                    state.value = ScoreQueueState.Error(t.message ?: "unknown error")
                }
            }

        return ScorePlaybackSession(
            state = state.asStateFlow(),
            events = events.asSharedFlow(),
            stopImpl = { stop() },
        )
    }
}

