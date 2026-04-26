package com.drumpractise.app.score

import android.util.LruCache
import com.drumpractise.app.score.nativenotation.VerovioScoreRuntime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.verovio.lib.toolkit

/**
 * LRU cache of Verovio SVG output by (cache key path or xml hash, scale percent).
 * [renderMutex] serializes toolkit + cache access. Rendering runs on [staffPreviewSvgRenderDispatcher]
 * so the main thread is not blocked during engraving.
 */
internal object StaffPreviewSvgCache {
    private const val MAX_ENTRIES = 48

    private val cache = LruCache<String, String>(MAX_ENTRIES)

    val renderMutex = Mutex()

    /** Single-thread pool: one Verovio toolkit instance; avoid overlapping native calls. */
    internal val staffPreviewSvgRenderDispatcher: CoroutineDispatcher =
        Dispatchers.Default.limitedParallelism(1)

    private const val EMPTY_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"

    fun cacheKeyFor(
        staffPreviewCacheKey: String?,
        musicXml: String,
        scalePercent: Int,
    ): String =
        if (staffPreviewCacheKey != null) {
            "$staffPreviewCacheKey|$scalePercent"
        } else {
            "xml:${musicXml.hashCode()}_${musicXml.length}|$scalePercent"
        }

    suspend fun renderToSvgOrCached(
        musicXml: String,
        staffPreviewCacheKey: String?,
        staffZoomScale: Float,
    ): String =
        withContext(staffPreviewSvgRenderDispatcher) {
            val xml = musicXml.trim()
            if (xml.isEmpty()) return@withContext EMPTY_SVG
            val scalePercent = (staffZoomScale.coerceIn(0.5f, 2.8f) * 100f).toInt().coerceIn(50, 280)
            val key = cacheKeyFor(staffPreviewCacheKey, xml, scalePercent)
            renderMutex.withLock {
                cache.get(key)?.let { return@withContext it }
                val tk = waitToolkit() ?: return@withContext EMPTY_SVG
                if (!tk.loadData(xml)) {
                    return@withContext EMPTY_SVG
                }
                tk.setOptions("""{"scale": $scalePercent}""")
                tk.redoLayout()
                val svg = tk.renderToSVG(1)
                cache.put(key, svg)
                svg
            }
        }

    suspend fun ensureRendered(
        relativePath: String,
        musicXml: String,
        scalePercent: Int,
    ) {
        withContext(staffPreviewSvgRenderDispatcher) {
            val xml = musicXml.trim()
            if (xml.isEmpty()) return@withContext
            val key = "$relativePath|$scalePercent"
            renderMutex.withLock {
                cache.get(key)?.let { return@withContext }
                val tk = waitToolkit() ?: return@withContext
                if (!tk.loadData(xml)) return@withContext
                tk.setOptions("""{"scale": $scalePercent}""")
                tk.redoLayout()
                val svg = tk.renderToSVG(1)
                cache.put(key, svg)
            }
        }
    }

    private suspend fun waitToolkit(): toolkit? {
        var tk = VerovioScoreRuntime.toolkitOrNull()
        var waitTicks = 0
        if (tk == null) {
            while (waitTicks < 200 && tk == null) {
                delay(16)
                tk = VerovioScoreRuntime.toolkitOrNull()
                waitTicks++
            }
        }
        return tk
    }
}
