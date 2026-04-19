package com.drumpractise.app.practice

/**
 * 预备拍「一二三四」：Android 播放 raw；其它平台可按 BPM 延迟并保持 [onBeat] 节拍位置。
 *
 * @param onBeat 主线程回调，参数为 1..4，与每拍 onset 对齐。
 */
expect suspend fun playPracticeCountIn(
    bpm: Int,
    onBeat: ((Int) -> Unit)? = null,
)
