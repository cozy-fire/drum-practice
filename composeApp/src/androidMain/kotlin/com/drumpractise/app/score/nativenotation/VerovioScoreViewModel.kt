package com.drumpractise.app.score.nativenotation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.verovio.lib.toolkit

/**
 * Wraps Verovio JNI [toolkit] for offline MEI / MusicXML / MXL rendering (SVG).
 * Behaviour aligned with [verovio-android-demo](https://github.com/rism-digital/verovio-android-demo).
 */
class VerovioScoreViewModel : ViewModel() {

    private var initialized = false
    private lateinit var resourcePath: String
    private lateinit var verovioToolkit: toolkit

    val svgString = mutableStateOf("<svg></svg>")
    val loadError = mutableStateOf<String?>(null)
    /** false 直至 Verovio 引擎与首帧 SVG 就绪，用于延迟创建 WebView、显示加载态 */
    val isEngineReady = mutableStateOf(false)
    val fontOptions = listOf("Leipzig", "Bravura", "Leland", "Petaluma")

    private var viewSize by mutableStateOf(IntSize.Zero)
    private var currentPage by mutableIntStateOf(1)
    private var scaleIndex by mutableIntStateOf(3)
    private val scaleValues = listOf(50, 60, 80, 100, 150, 200)
    private var selectedFont = "Leipzig"

    suspend fun initIfNeeded(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        isEngineReady.value = false
        loadError.value = null
        try {
            val targetDir = File(app.filesDir, "verovio/data")
            resourcePath = targetDir.absolutePath

            VerovioDataUnpacker.ensureUnpacked(app)

            withContext(Dispatchers.Main) {
                verovioToolkit = toolkit(false)
                verovioToolkit.setResourcePath(resourcePath)
                verovioToolkit.setOptions("{'svgViewBox': 'true'}")
                verovioToolkit.setOptions("{'scaleToPageSize': 'true'}")
                verovioToolkit.setOptions("{'adjustPageHeight': 'true'}")
                verovioToolkit.setOptions("{'fontTextLiberation': 'true'}")

                val inputStream = app.assets.open("verovio-sample.mei")
                val mei = inputStream.bufferedReader().use { it.readText() }
                if (!verovioToolkit.loadData(mei)) {
                    loadError.value = "默认示例谱 loadData 失败"
                } else {
                    updateSvg()
                }
                initialized = true
                isEngineReady.value = true
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                loadError.value = e.message ?: e.toString()
                svgString.value = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                if (::verovioToolkit.isInitialized) {
                    verovioToolkit.delete()
                }
                isEngineReady.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (initialized) {
            verovioToolkit.delete()
        }
    }

    fun canPrevious(): Boolean = initialized && currentPage > 1

    fun canNext(): Boolean = initialized && currentPage < verovioToolkit.getPageCount()

    fun canZoomOut(): Boolean = initialized && scaleIndex > 1

    fun canZoomIn(): Boolean = initialized && scaleIndex < scaleValues.size - 1

    fun verovioVersion(): String = if (initialized) verovioToolkit.getVersion() else ""

    fun onPrevious() {
        if (!initialized) return
        if (canPrevious()) {
            currentPage--
            updateSvg()
        }
    }

    fun onNext() {
        if (!initialized) return
        if (canNext()) {
            currentPage++
            updateSvg()
        }
    }

    fun onZoomOut() {
        if (!initialized) return
        if (canZoomOut()) {
            scaleIndex--
            applyZoom()
        }
    }

    fun onZoomIn() {
        if (!initialized) return
        if (canZoomIn()) {
            scaleIndex++
            applyZoom()
        }
    }

    fun onFontSelect(font: String) {
        if (!initialized) return
        if (selectedFont != font) {
            selectedFont = font
            applyFont()
        }
    }

    fun onViewportSize(size: IntSize) {
        if (viewSize == size) return
        viewSize = size
        if (initialized && size.width > 0 && size.height > 0) {
            applySize()
        }
    }

    fun onLoadFile(absolutePath: String) {
        if (!initialized) return
        loadError.value = null
        val ok = verovioToolkit.loadFile(absolutePath)
        if (!ok) {
            loadError.value = "无法加载文件（Verovio loadFile 失败）"
            return
        }
        currentPage = 1
        updateSvg()
    }

    fun loadMusicXmlString(xml: String) {
        if (!initialized) return
        loadError.value = null
        val ok = verovioToolkit.loadData(xml)
        if (!ok) {
            loadError.value = "无法解析 MusicXML 字符串"
            return
        }
        currentPage = 1
        updateSvg()
    }

    private fun applyFont() {
        if (!initialized) return
        val scaleOptionsJSON = """{"font": "$selectedFont"}"""
        verovioToolkit.setOptions(scaleOptionsJSON)
        verovioToolkit.redoLayout()
        if (verovioToolkit.getPageCount() < currentPage) {
            currentPage = verovioToolkit.getPageCount()
        }
        updateSvg()
    }

    private fun applySize() {
        if (!initialized) return
        val height = 2100f * viewSize.height / viewSize.width.coerceAtLeast(1)
        val sizeJSON = """{"pageHeight": $height}"""
        verovioToolkit.setOptions(sizeJSON)
        applyZoom()
    }

    private fun applyZoom() {
        if (!initialized) return
        val scaleOptionsJSON = """{"scale": ${scaleValues[scaleIndex]}}"""
        verovioToolkit.setOptions(scaleOptionsJSON)
        verovioToolkit.redoLayout()
        if (verovioToolkit.getPageCount() < currentPage) {
            currentPage = verovioToolkit.getPageCount()
        }
        updateSvg()
    }

    private fun updateSvg() {
        if (!initialized) return
        svgString.value = verovioToolkit.renderToSVG(currentPage)
    }
}
