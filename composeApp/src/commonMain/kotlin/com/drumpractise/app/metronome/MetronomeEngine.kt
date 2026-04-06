package com.drumpractise.app.metronome

expect class MetronomeEngine() {
    fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit)

    fun stop()

    fun updateConfig(config: MetronomeRunConfig)

    /** 预加载采样等资源，降低首次播放延迟（Android：解码 raw → PCM，在 IO 调度器执行）。 */
    suspend fun warmUp()

    fun release()
}
