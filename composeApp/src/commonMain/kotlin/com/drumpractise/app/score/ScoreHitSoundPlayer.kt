package com.drumpractise.app.score

import androidx.compose.runtime.Composable

/** Android：解析 MusicXML、按采样时钟经 [android.media.AudioTrack] 混音播放；其他平台为空操作。 */
interface ScoreHitSoundPlayer {
    /**
     * Whether this player can accurately report completion for a finite number of loops.
     *
     * If false, callers should fall back to time-based switching when they need sequencing.
     */
    val supportsFiniteCompletion: Boolean
        get() = false

    fun startPlayback(musicXml: String, bpm: Int)

    /**
     * Play the given score for exactly [loopCount] loops and then call [onCompleted] (if supported).
     *
     * Default implementation falls back to [startPlayback] and never calls [onCompleted].
     */
    fun startPlaybackFinite(
        musicXml: String,
        bpm: Int,
        loopCount: Int,
        onCompleted: (() -> Unit)? = null,
    ) {
        startPlayback(musicXml, bpm)
    }

    fun stopPlayback()

    /** 预解码映射内全部 raw，避免首次 [startPlayback] 在音频线程阻塞。 */
    suspend fun warmup()
}

/** 非 Android 目标使用；不打断、不发声。 */
object NoOpScoreHitSoundPlayer : ScoreHitSoundPlayer {
    override val supportsFiniteCompletion: Boolean = false

    override fun startPlayback(musicXml: String, bpm: Int) {}

    override fun stopPlayback() {}

    override suspend fun warmup() {}
}

@Composable
expect fun rememberScoreHitSoundPlayer(): ScoreHitSoundPlayer
