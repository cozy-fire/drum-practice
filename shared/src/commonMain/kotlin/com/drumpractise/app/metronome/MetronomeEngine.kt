package com.drumpractise.app.metronome

expect class MetronomeEngine {
    fun start(bpm: Int)
    fun stop()
}
