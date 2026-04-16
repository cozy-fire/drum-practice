package com.drumpractise.app.metronome

expect object MetronomeBackgroundController {
    fun start(config: MetronomeRunConfig)

    fun stop()

    fun updateConfig(config: MetronomeRunConfig)

    fun setOnBeatListener(onBeat: ((Int, MetronomeAccent) -> Unit)?)
}

