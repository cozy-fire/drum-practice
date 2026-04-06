package com.drumpractise.app.score.nativenotation

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.verovio.lib.toolkit

/**
 * 进程级 Verovio 初始化，供 [App] 启动预热与 [VerovioScoreViewModel] 共用同一 [toolkit]。
 * 不在页面 [onCleared] 中 [toolkit.delete]。
 */
object VerovioScoreRuntime {
    private val mutex = Mutex()

    @Volatile
    private var tk: toolkit? = null

    private var pathStr: String = ""

    @Volatile
    var initialSvg: String = "<svg></svg>"
        private set

    @Volatile
    var initialLoadError: String? = null
        private set

    fun toolkitOrNull(): toolkit? = tk

    fun resourcePath(): String = pathStr

    suspend fun warmUp(app: Context) {
        mutex.withLock {
            if (tk != null) return
            try {
                withContext(Dispatchers.IO) {
                    VerovioDataUnpacker.ensureUnpacked(app)
                }
                withContext(Dispatchers.Main) {
                    val targetDir = File(app.filesDir, "verovio/data")
                    pathStr = targetDir.absolutePath
                    val t = toolkit(false)
                    t.setResourcePath(pathStr)
                    t.setOptions("{'svgViewBox': 'true'}")
                    t.setOptions("{'scaleToPageSize': 'true'}")
                    t.setOptions("{'adjustPageHeight': 'true'}")
                    t.setOptions("{'fontTextLiberation': 'true'}")

                    val inputStream = app.assets.open("verovio-sample.mei")
                    val mei = inputStream.bufferedReader().use { it.readText() }
                    if (!t.loadData(mei)) {
                        initialLoadError = "默认示例谱 loadData 失败"
                    } else {
                        initialLoadError = null
                    }
                    initialSvg = t.renderToSVG(1)
                    tk = t
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    initialLoadError = e.message ?: e.toString()
                    initialSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                    tk?.delete()
                    tk = null
                }
            }
        }
    }
}
