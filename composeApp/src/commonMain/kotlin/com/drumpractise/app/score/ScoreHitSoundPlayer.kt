package com.drumpractise.app.score

import androidx.compose.runtime.Composable

/** Android：解析 MusicXML、按采样时钟经 [android.media.AudioTrack] 混音播放；其他平台为空操作。 */
interface ScoreHitSoundPlayer {
    fun startPlayback(musicXml: String, bpm: Int)

    fun stopPlayback()

    /** 预解码映射内全部 raw，避免首次 [startPlayback] 在音频线程阻塞。 */
    suspend fun warmup()
}

/** 非 Android 目标使用；不打断、不发声。 */
object NoOpScoreHitSoundPlayer : ScoreHitSoundPlayer {
    override fun startPlayback(musicXml: String, bpm: Int) {}

    override fun stopPlayback() {}

    override suspend fun warmup() {}
}

@Composable
expect fun rememberScoreHitSoundPlayer(): ScoreHitSoundPlayer
