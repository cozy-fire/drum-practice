package com.drumpractise.app.metronome

/**
 * 后台播放开关开启时的播放控制入口。
 *
 * - Android：转发到 Foreground Service（mediaPlayback）
 * - Desktop：进程内继续播放
 */
expect object MetronomeBackgroundController {
    fun start(config: MetronomeRunConfig)

    fun stop()

    fun updateConfig(config: MetronomeRunConfig)
}

