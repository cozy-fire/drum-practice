package com.drumpractise.app.score

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drumpractise.app.score.nativenotation.VerovioScoreViewModel
import java.io.File
import java.io.FileOutputStream

private const val SAMPLE_MUSIC_XML =
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
      <note>
        <pitch><step>C</step><octave>4</octave></pitch>
        <duration>4</duration>
        <type>whole</type>
      </note>
    </measure>
  </part>
</score-partwise>"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun MusicXmlScoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: VerovioScoreViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.initIfNeeded(context.applicationContext)
    }

    val openDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                copyUriToTempFile(context, it)?.let { file ->
                    viewModel.onLoadFile(file.absolutePath)
                }
            }
        }

    var fontMenuExpanded by remember { mutableStateOf(false) }

    val engineReady by viewModel.isEngineReady

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text("离线五线谱 (Verovio)") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onPrevious() },
                        enabled = engineReady && viewModel.canPrevious(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一页")
                    }
                    IconButton(
                        onClick = { viewModel.onNext() },
                        enabled = engineReady && viewModel.canNext(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一页")
                    }
                    IconButton(
                        onClick = { viewModel.onZoomOut() },
                        enabled = engineReady && viewModel.canZoomOut(),
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "缩小")
                    }
                    IconButton(
                        onClick = { viewModel.onZoomIn() },
                        enabled = engineReady && viewModel.canZoomIn(),
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "放大")
                    }
                    Box {
                        TextButton(
                            onClick = { fontMenuExpanded = true },
                            enabled = engineReady,
                        ) { Text("字体") }
                        DropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { fontMenuExpanded = false },
                        ) {
                            viewModel.fontOptions.forEach { font ->
                                DropdownMenuItem(
                                    text = { Text(font) },
                                    onClick = {
                                        viewModel.onFontSelect(font)
                                        fontMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            openDocument.launch(
                                arrayOf(
                                    "application/vnd.recordare.musicxml",
                                    "application/xml",
                                    "text/xml",
                                    "application/octet-stream",
                                    "application/x-zip-compressed",
                                ),
                            )
                        },
                        enabled = engineReady,
                    ) {
                        Text("打开文件")
                    }
                    TextButton(
                        onClick = { viewModel.loadMusicXmlString(SAMPLE_MUSIC_XML) },
                        enabled = engineReady,
                    ) {
                        Text("示例 XML")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (!engineReady) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.loadError.value?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    VerovioSvgWebView(
                        svgContent = viewModel.svgString.value,
                        onViewportSizeChanged = viewModel::onViewportSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VerovioSvgWebView(
    svgContent: String,
    onViewportSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val html =
        remember(svgContent) {
            """
            <!DOCTYPE html>
            <html>
            <head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0">
            $svgContent
            </body>
            </html>
            """.trimIndent()
        }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier =
            modifier.onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    onViewportSizeChanged(size)
                }
            },
    )
}

private fun copyUriToTempFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "score_temp"
        val tempFile = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
