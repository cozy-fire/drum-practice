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
 *
 * 预热时加载 **空白 MusicXML**（无音符）；示例谱见 assets [ASSET_SAMPLE_MUSICXML]。
 */
object VerovioScoreRuntime {

    /** 与界面「示例 XML」一致，由 [readSampleMusicXml] 读取。 */
    const val ASSET_SAMPLE_MUSICXML: String = "verovio-sample.musicxml"

    /** 默认空白谱（有拍号调号谱表，无音符），供 warmUp 首次 loadData。 */
    private const val BLANK_DEFAULT_MUSIC_XML: String =
        """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.1 Partwise//EN" "http://www.musicxml.org/dtds/partwise.dtd">
<score-partwise version="3.1">
  <part-list>
    <score-part id="P1"><part-name>Music</part-name></score-part>
  </part-list>
  <part id="P1">
    <measure number="1">
      <attributes>
        <divisions>1</divisions>
        <key><fifths>0</fifths></key>
        <time><beats>4</beats><beat-type>4</beat-type></time>
        <clef><sign>G</sign><line>2</line></clef>
      </attributes>
    </measure>
  </part>
</score-partwise>"""

    fun readSampleMusicXml(context: Context): String =
        context.assets.open(ASSET_SAMPLE_MUSICXML).bufferedReader().use { it.readText() }
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

                    if (!t.loadData(BLANK_DEFAULT_MUSIC_XML)) {
                        initialLoadError = "默认空白谱 loadData 失败"
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
