package com.drumpractise.app.score

import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.musicxml.MusicXmlRepository
import com.drumpractise.app.score.musicxml.buildDrumScorePlaybackSchedule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class MusicXmlQueueItem(
    val id: String,
    /** Relative to composeResources/files/ */
    val musicXmlPath: String,
    /** When non-null and non-blank, used instead of loading from [musicXmlPath]. */
    val inlineMusicXml: String? = null,
)

suspend fun MusicXmlQueueItem.resolveMusicXml(): String {
    val inline = inlineMusicXml?.trim().orEmpty()
    if (inline.isNotEmpty()) return inline
    return MusicXmlRepository.getXml(musicXmlPath)
}

/**
 * Application-wide score hit playback state and control. All playing states are expressed as a
 * queue (single-segment playback is `items.size == 1`).
 */
object ScorePlaybackController {
    private val hitSound: ScoreHitSoundPlayer
        get() = globalScoreHitSoundPlayer()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val _uiState = MutableStateFlow<ScorePlaybackUiState>(ScorePlaybackUiState.Idle)
    val uiState: StateFlow<ScorePlaybackUiState> = _uiState.asStateFlow()

    suspend fun warmup() {
        hitSound.warmup()
    }

    /**
     * Starts playback of [items] from [startIndex]. Cancels any current session.
     * Use [loopCount] per item as in [ScoreHitSoundPlayer.startPlaybackFinite] (e.g. [Int.MAX_VALUE] for loop until stop).
     */
    fun playQueue(
        items: List<MusicXmlQueueItem>,
        bpm: Int,
        loopCount: Int,
        pcmSampleRate: Int = 48_000,
        startIndex: Int = 0,
        /** 非 null 时启用谱面重音与弱音倍率（仅重音移位等显式传入）。 */
        weakNoteVolumeScale: Float? = null,
    ) {
        job?.cancel()
        hitSound.stopPlayback()
        job = null

        if (items.isEmpty()) {
            _uiState.value = ScorePlaybackUiState.Idle
            return
        }

        val from = startIndex.coerceIn(0, items.lastIndex)
        val loops = loopCount.coerceAtLeast(1)

        job =
            scope.launch {
                try {
                    for (idx in from until items.size) {
                        val item = items[idx]
                        _uiState.value =
                            ScorePlaybackUiState.Playing(
                                index = idx,
                                total = items.size,
                                itemId = item.id,
                                bpm = bpm,
                                loopCount = loops,
                            )

                        val xml = item.resolveMusicXml()
                        if (xml.isBlank()) {
                            continue
                        }

                        if (hitSound.supportsFiniteCompletion && hitSound !== NoOpScoreHitSoundPlayer) {
                            suspendCancellableCoroutine { cont ->
                                cont.invokeOnCancellation { hitSound.stopPlayback() }
                                hitSound.startPlaybackFinite(
                                    musicXml = xml,
                                    bpm = bpm,
                                    loopCount = loops,
                                    onCompleted = { cont.resume(Unit) },
                                    weakNoteVolumeScale = weakNoteVolumeScale,
                                )
                            }
                        } else {
                            val schedule =
                                buildDrumScorePlaybackSchedule(
                                    parsed = MusicXmlDrumTimelineParser.parse(xml),
                                    bpm = bpm,
                                    pcmSampleRate = pcmSampleRate,
                                    weakNoteVolumeScale = weakNoteVolumeScale,
                                )
                            if (schedule != null) {
                                val loopMs =
                                    (schedule.loopLengthSamples / pcmSampleRate.toDouble() * 1000.0)
                                        .coerceAtLeast(1.0)
                                hitSound.startPlayback(xml, bpm, weakNoteVolumeScale)
                                delay((loopMs * loops).toLong())
                            }
                        }
                    }

                    hitSound.stopPlayback()
                    _uiState.value = ScorePlaybackUiState.Idle
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    hitSound.stopPlayback()
                    _uiState.value = ScorePlaybackUiState.Error(t.message ?: "unknown error")
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        hitSound.stopPlayback()
        _uiState.value = ScorePlaybackUiState.Idle
    }

    /** Stops audio but keeps [ScorePlaybackUiState.Paused] for resume (same behavior as previous separation pause). */
    fun pause() {
        val st = _uiState.value
        if (st !is ScorePlaybackUiState.Playing) return
        val idx = st.index
        job?.cancel()
        job = null
        hitSound.stopPlayback()
        // Caller must pass the same item list + params on next playQueue; we only store resume index.
        _uiState.value =
            ScorePlaybackUiState.Paused(
                resumeIndex = idx,
                bpm = st.bpm,
                loopCount = st.loopCount,
            )
    }
}

sealed interface ScorePlaybackUiState {
    data object Idle : ScorePlaybackUiState

    data class Playing(
        val index: Int,
        val total: Int,
        val itemId: String,
        val bpm: Int,
        val loopCount: Int,
    ) : ScorePlaybackUiState

    data class Paused(
        val resumeIndex: Int,
        val bpm: Int,
        val loopCount: Int,
    ) : ScorePlaybackUiState

    data class Error(
        val message: String,
    ) : ScorePlaybackUiState
}
