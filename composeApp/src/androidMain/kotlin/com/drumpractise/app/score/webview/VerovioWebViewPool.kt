package com.drumpractise.app.score.webview

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Verovio 谱面用 WebView 对象池：Application 级单例，主线程访问。
 *
 * 定标见 WebView 池化方案：`pool_max_size=3`、`pool_warm_min=1`、idle TTL 5min、
 * 池满后 `acquire` 使用 ephemeral 实例（release 时 destroy）。
 */
object VerovioWebViewPool {

    const val POOL_MAX_SIZE: Int = 3
    const val POOL_WARM_MIN: Int = 1
    const val POOL_INITIAL_PREWARM: Int = 1
    private const val IDLE_TTL_MS: Long = 300_000L
    private const val IDLE_SWEEP_INTERVAL_MS: Long = 60_000L
    private const val TAG: String = "DrumWebViewPool"

    /** debuggable 包打详细日志；亦可用 `adb shell setprop log.tag.DrumWebViewPool DEBUG`。 */
    private fun logVerbose(): Boolean {
        if (Log.isLoggable(TAG, Log.DEBUG)) return true
        return runCatching {
            appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }.getOrDefault(false)
    }

    internal const val MINIMAL_HTML_SHELL: String =
        """<!DOCTYPE html>
<html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0"></body></html>"""

    /**
     * 归还池前加载的页面：结构与谱面页 [loadDataWithBaseURL] 一致，正文为空白 SVG。
     * （WebView 不直接渲染 MusicXML；无内容谱面在 UI 上等价于空 SVG，便于下次进入时不短暂露出旧谱。）
     */
    private const val BLANK_SCORE_HTML: String =
        """<!DOCTYPE html>
<html>
<head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0">
<svg xmlns="http://www.w3.org/2000/svg"></svg>
</body>
</html>"""

    private lateinit var appContext: android.content.Context
    private lateinit var applicationScope: CoroutineScope

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val idle: ArrayDeque<WebView> = ArrayDeque()
    private val idleSince: IdentityHashMap<WebView, Long> = IdentityHashMap()
    private val borrowedPooled: MutableSet<WebView> = Collections.newSetFromMap(IdentityHashMap())
    private val ephemeralBorrowed: MutableSet<WebView> = Collections.newSetFromMap(IdentityHashMap())

    private val idleSweepRunnable =
        object : Runnable {
            override fun run() {
                synchronized(lock) {
                    sweepIdleTimeoutsLocked()
                }
                scheduleNextIdleSweepIfNeeded()
            }
        }

    private var installed: Boolean = false

    fun install(
        application: Application,
        scope: CoroutineScope,
    ) {
        synchronized(lock) {
            if (installed) return
            appContext = application.applicationContext
            applicationScope = scope
            installed = true
        }
        application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}

                override fun onLowMemory() {
                    onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
                }

                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                        trimIdleAggressive()
                    }
                }
            },
        )
        log("install")
    }

    fun prewarmAsync() {
        applicationScope.launch(Dispatchers.Main) {
            yield()
            prewarmInternal()
        }
    }

    private fun prewarmInternal() {
        checkMainThread()
        synchronized(lock) {
            if (!installed) return
            if (idle.size >= POOL_INITIAL_PREWARM) return
            val total = idle.size + borrowedPooled.size
            if (total >= POOL_MAX_SIZE) return
            val w = createConfiguredWebView()
            w.loadDataWithBaseURL(null, MINIMAL_HTML_SHELL, "text/html", "UTF-8", null)
            idle.addLast(w)
            idleSince[w] = SystemClock.elapsedRealtime()
            log("prewarm pooled idle=${idle.size} borrowed=${borrowedPooled.size}")
        }
        scheduleNextIdleSweepIfNeeded()
    }

    fun acquire(): WebView {
        checkMainThread()
        synchronized(lock) {
            check(installed) { "VerovioWebViewPool.install() must be called from Application" }
            sweepIdleTimeoutsLocked()
            if (idle.isNotEmpty()) {
                val w = idle.removeFirst()
                idleSince.remove(w)
                borrowedPooled.add(w)
                log("acquire hit_idle")
                return w
            }
            val total = idle.size + borrowedPooled.size
            if (total < POOL_MAX_SIZE) {
                val w = createConfiguredWebView()
                borrowedPooled.add(w)
                log("acquire miss_new_pooled total=${total + 1}")
                return w
            }
            val w = createConfiguredWebView()
            ephemeralBorrowed.add(w)
            log("acquire ephemeral")
            return w
        }
    }

    fun release(webView: WebView) {
        checkMainThread()
        synchronized(lock) {
            if (!installed) {
                runCatching { webView.destroy() }
                return
            }
            when {
                ephemeralBorrowed.remove(webView) -> {
                    runCatching { webView.destroy() }
                    log("release ephemeral destroyed")
                }
                borrowedPooled.remove(webView) -> {
                    runCatching { resetForPool(webView) }
                    idle.addLast(webView)
                    idleSince[webView] = SystemClock.elapsedRealtime()
                    log("release to_idle idle=${idle.size}")
                    scheduleNextIdleSweepIfNeeded()
                    ensureWarmMinAsyncLocked()
                }
                idle.contains(webView) -> {
                    log("release noop already_idle")
                }
                else -> {
                    runCatching { webView.destroy() }
                    log("release unknown destroyed")
                }
            }
        }
    }

    private fun trimIdleAggressive() {
        mainHandler.post {
            synchronized(lock) {
                if (idle.isEmpty()) return@synchronized
                val copy = idle.toList()
                idle.clear()
                idleSince.clear()
                for (w in copy) {
                    runCatching { w.destroy() }
                }
                log("trimMemory critical idle_cleared count=${copy.size}")
            }
            mainHandler.removeCallbacks(idleSweepRunnable)
        }
    }

    private fun sweepIdleTimeoutsLocked() {
        val now = SystemClock.elapsedRealtime()
        val snapshot = idle.toList()
        val toDestroy = mutableListOf<WebView>()
        for (w in snapshot) {
            if (!idle.contains(w)) continue
            val since = idleSince[w] ?: continue
            if (now - since >= IDLE_TTL_MS) {
                toDestroy.add(w)
            }
        }
        for (w in toDestroy) {
            idle.remove(w)
            idleSince.remove(w)
            runCatching { w.destroy() }
            log("idle_ttl destroy")
        }
        if (toDestroy.isNotEmpty()) {
            ensureWarmMinAsyncLocked()
        }
    }

    private fun ensureWarmMinAsyncLocked() {
        val shortfall = POOL_WARM_MIN - (idle.size + borrowedPooled.size)
        if (shortfall <= 0) return
        applicationScope.launch(Dispatchers.Main) {
            synchronized(lock) {
                var need = POOL_WARM_MIN - (idle.size + borrowedPooled.size)
                while (need > 0 && idle.size + borrowedPooled.size < POOL_MAX_SIZE) {
                    val w = createConfiguredWebView()
                    w.loadDataWithBaseURL(null, MINIMAL_HTML_SHELL, "text/html", "UTF-8", null)
                    idle.addLast(w)
                    idleSince[w] = SystemClock.elapsedRealtime()
                    need--
                    log("warm_min replenish")
                }
            }
            scheduleNextIdleSweepIfNeeded()
        }
    }

    private fun scheduleNextIdleSweepIfNeeded() {
        mainHandler.removeCallbacks(idleSweepRunnable)
        synchronized(lock) {
            if (idle.isEmpty()) return
        }
        mainHandler.postDelayed(idleSweepRunnable, IDLE_SWEEP_INTERVAL_MS)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(): WebView {
        return WebView(appContext).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
        }
    }

    private fun resetForPool(w: WebView) {
        w.loadDataWithBaseURL(null, BLANK_SCORE_HTML, "text/html", "UTF-8", null)
    }

    private fun checkMainThread() {
        check(Looper.getMainLooper() == Looper.myLooper()) {
            "VerovioWebViewPool must be used on main thread"
        }
    }

    private fun log(msg: String) {
        if (logVerbose()) {
            Log.d(TAG, msg)
        }
    }
}
