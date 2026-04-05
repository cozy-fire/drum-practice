package com.drumpractise.app.metronome

expect class MetronomeEngine() {
    fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit)

    fun stop()

    fun updateConfig(config: MetronomeRunConfig)

    /** 预加载采样等资源，降低首次播放延迟（Android：SoundPool）。 */
    fun warmUp()

    fun release()
}
