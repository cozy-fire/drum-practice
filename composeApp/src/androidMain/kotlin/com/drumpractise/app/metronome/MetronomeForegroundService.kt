package com.drumpractise.app.metronome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.drumpractise.app.MainActivity
import com.drumpractise.app.R
import com.drumpractise.app.settings.AppSettings

class MetronomeForegroundService : Service() {
    private val engine = MetronomeEngine()
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val cfg = configFromIntent(intent) ?: return START_NOT_STICKY
                startForegroundInternal()
                if (running) {
                    engine.updateConfig(cfg)
                } else {
                    startEngine(cfg)
                }
            }
            ACTION_UPDATE -> {
                val cfg = configFromIntent(intent) ?: return START_NOT_STICKY
                if (running) {
                    engine.updateConfig(cfg)
                } else {
                    startForegroundInternal()
                    startEngine(cfg)
                }
            }
            ACTION_STOP -> {
                stopEngine()
                stopSelf()
            }
            else -> Unit
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopEngine()
        engine.release()
        super.onDestroy()
    }

    private fun startEngine(config: MetronomeRunConfig) {
        if (running) return
        running = true
        AppSettings.setMetronomeBackgroundRunning(true)
        engine.start(config) { index, tier ->
            MetronomeBackgroundController.emitBeat(index, tier)
        }
    }

    private fun stopEngine() {
        if (!running) return
        running = false
        AppSettings.setMetronomeBackgroundRunning(false)
        engine.stop()
    }

    private fun startForegroundInternal() {
        ensureNotificationChannel()
        val notification = buildNotification()
        val fgsType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            fgsType,
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "节拍器",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "节拍器后台播放"
            }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val openPending =
            PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )

        val stopIntent =
            Intent(this, MetronomeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
        val stopPending =
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("节拍器后台播放中")
            .setContentText("点击可返回应用")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "停止",
                    stopPending,
                ).build(),
            )
            .build()
    }

    private fun pendingIntentImmutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun configFromIntent(intent: Intent): MetronomeRunConfig? {
        val bpm = intent.getIntExtra(EXTRA_BPM, -1)
        val noteDivisor = intent.getIntExtra(EXTRA_NOTE_DIVISOR, -1)
        val presetOrdinal = intent.getIntExtra(EXTRA_PRESET_ORDINAL, -1)
        if (bpm <= 0 || noteDivisor <= 0 || presetOrdinal < 0) return null
        val preset = MetronomeSoundPreset.entries.getOrNull(presetOrdinal) ?: return null
        return MetronomeRunConfig(bpm = bpm, noteDivisor = noteDivisor, preset = preset)
    }

    companion object {
        const val ACTION_START = "com.drumpractise.app.metronome.action.START"
        const val ACTION_UPDATE = "com.drumpractise.app.metronome.action.UPDATE"
        const val ACTION_STOP = "com.drumpractise.app.metronome.action.STOP"

        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_NOTE_DIVISOR = "extra_note_divisor"
        const val EXTRA_PRESET_ORDINAL = "extra_preset_ordinal"

        private const val CHANNEL_ID = "metronome_playback"
        private const val NOTIFICATION_ID = 1001
    }
}

