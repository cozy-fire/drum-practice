package com.drumpractise.app.metronome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioFrameCount
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioTime
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL

actual class MetronomeEngine actual constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    @Volatile private var running = false
    private var storedOnBeat: ((Int, MetronomeAccent) -> Unit)? = null

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var outputSampleRate: Double = 44100.0
    private val buffers =
        Array(MetronomeSoundPreset.entries.size) {
            arrayOfNulls<AVAudioPCMBuffer>(MetronomeAccent.entries.size)
        }

    actual fun start(config: MetronomeRunConfig, onBeat: (indexInPeriod: Int, tier: MetronomeAccent) -> Unit) {
        stop()
        storedOnBeat = onBeat
        running = true
        val intervalMs = metronomeIntervalMs(config.bpm, config.noteDivisor)
        val period = metronomeBeatPeriod(config.noteDivisor)
        val presetOrdinal = config.preset.ordinal
        job =
            scope.launch {
                if (!ensureEngineAndBuffers()) {
                    running = false
                    return@launch
                }
                val player = playerNode ?: return@launch
                val sr = outputSampleRate
                val samplesPerBeat = (intervalMs / 1000.0 * sr).toLong().coerceAtLeast(1L)
                var nextSampleTime = 0L
                var beat = 0
                var nextDeadlineMs = uptimeMs()
                while (isActive && running) {
                    val now = uptimeMs()
                    val sleepMs = (nextDeadlineMs - now).toLong().coerceAtLeast(0L)
                    if (sleepMs > 0) {
                        delay(sleepMs)
                    }
                    if (!isActive || !running) break
                    val indexInPeriod = beat % period
                    val tier = metronomeAccent(indexInPeriod, config.noteDivisor)
                    val buf = buffers[presetOrdinal][tier.ordinal]
                    if (buf != null) {
                        val whenTime = AVAudioTime.timeWithSampleTime(nextSampleTime, atRate = sr)
                        player.scheduleBuffer(buf, atTime = whenTime, options = 0u, completionHandler = null)
                    }
                    nextSampleTime += samplesPerBeat
                    withContext(Dispatchers.Main) {
                        onBeat(indexInPeriod, tier)
                    }
                    beat++
                    nextDeadlineMs += intervalMs
                }
            }
    }

    actual fun stop() {
        running = false
        job?.cancel()
        job = null
        playerNode?.reset()
    }

    actual fun updateConfig(config: MetronomeRunConfig) {
        val cb = storedOnBeat ?: return
        if (!running) return
        stop()
        running = true
        start(config, cb)
    }

    actual fun warmUp() {}

    actual fun release() {
        stop()
        tearDownAudio()
        storedOnBeat = null
    }

    private fun uptimeMs(): Double = NSProcessInfo.processInfo.systemUptime * 1000.0

    private fun ensureEngineAndBuffers(): Boolean {
        if (engine != null && playerNode != null) {
            loadAllBuffersFromBundle()
            return true
        }
        val bundle = NSBundle.mainBundle
        var wireFormat: AVAudioFormat? = null
        outer@ for (preset in MetronomeSoundPreset.entries) {
            for (tier in MetronomeAccent.entries) {
                val name = metronomeSampleBaseName(preset, tier)
                val path = bundle.pathForResource(name, ofType = "wav") ?: continue
                val url = NSURL.fileURLWithPath(path)
                val file = AVAudioFile(forReading = url, error = null) ?: continue
                wireFormat = file.processingFormat
                break@outer
            }
        }
        if (wireFormat == null) return false

        val session = AVAudioSession.sharedInstance()
        session.setCategory(platform.AVFAudio.AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)

        val eng = AVAudioEngine()
        val player = AVAudioPlayerNode()
        eng.attachNode(player)
        eng.connect(player, eng.mainMixerNode, format = wireFormat)
        outputSampleRate = wireFormat.sampleRate

        eng.prepare()
        eng.startAndReturnError(null)
        player.play()

        engine = eng
        playerNode = player

        loadAllBuffersFromBundle()
        return true
    }

    private fun loadAllBuffersFromBundle() {
        val bundle = NSBundle.mainBundle
        for (preset in MetronomeSoundPreset.entries) {
            for (tier in MetronomeAccent.entries) {
                val p = preset.ordinal
                val t = tier.ordinal
                if (buffers[p][t] != null) continue
                val name = metronomeSampleBaseName(preset, tier)
                val path = bundle.pathForResource(name, ofType = "wav") ?: continue
                val url = NSURL.fileURLWithPath(path)
                val file = AVAudioFile(forReading = url, error = null) ?: continue
                val fileFormat = file.processingFormat
                val nFrames = file.length.toUInt()
                if (nFrames == 0u) continue
                val pcm =
                    AVAudioPCMBuffer(
                        pcmFormat = fileFormat,
                        frameCapacity = AVAudioFrameCount(nFrames),
                    ) ?: continue
                file.readIntoBuffer(pcm, error = null)
                buffers[p][t] = pcm
            }
        }
    }

    private fun tearDownAudio() {
        playerNode?.stop()
        engine?.stop()
        playerNode = null
        engine = null
        for (p in buffers.indices) {
            for (t in buffers[p].indices) {
                buffers[p][t] = null
            }
        }
    }
}
