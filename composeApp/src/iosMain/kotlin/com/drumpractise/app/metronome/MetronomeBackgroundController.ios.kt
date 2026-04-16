package com.drumpractise.app.metronome

actual object MetronomeBackgroundController {
    private val engine = MetronomeEngine()
    private var running = false

    actual fun start(config: MetronomeRunConfig) {
        if (running) {
            engine.updateConfig(config)
            return
        }
        running = true
        engine.start(config) { _, _ -> }
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
}

