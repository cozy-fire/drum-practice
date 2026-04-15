package com.drumpractise.app.metronome

actual object MetronomeBackgroundController {
    private val engine = MetronomeEngine()

    actual fun start(config: MetronomeRunConfig) {
        engine.start(config) { _, _ -> }
    }

    actual fun stop() {
        engine.stop()
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        engine.updateConfig(config)
    }
}

