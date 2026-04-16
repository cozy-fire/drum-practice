package com.drumpractise.app.metronome

actual object MetronomeBackgroundController {
    private val engine = MetronomeEngine()
    private var running = false
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null

    actual fun start(config: MetronomeRunConfig) {
        if (running) {
            engine.updateConfig(config)
            return
        }
        running = true
        engine.start(config) { index, tier ->
            storedOnBeat?.invoke(index, tier)
        }
    }

    actual fun stop() {
        if (!running) return
        running = false
        engine.stop()
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        if (!running) return
        engine.updateConfig(config)
    }

    actual fun setOnBeatListener(onBeat: ((Int, MetronomeAccent) -> Unit)?) {
        storedOnBeat = onBeat
    }
}

