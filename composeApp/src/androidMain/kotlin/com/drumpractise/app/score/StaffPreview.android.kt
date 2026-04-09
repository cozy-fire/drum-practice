package com.drumpractise.app.score

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drumpractise.app.score.webview.VerovioWebViewPool

@Composable
actual fun StaffPreview(
    musicXml: String,
    playbackHighlight: Boolean,
    modifier: Modifier,
    staffPreviewCacheKey: String?,
) {
    var svgContent by remember { mutableStateOf("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>") }
    val staffZoomScale by StaffZoomStore.staffZoomScale.collectAsState()

    LaunchedEffect(musicXml, staffZoomScale, staffPreviewCacheKey) {
        svgContent =
            StaffPreviewSvgCache.renderToSvgOrCached(
                musicXml = musicXml,
                staffPreviewCacheKey = staffPreviewCacheKey,
                staffZoomScale = staffZoomScale,
            )
    }

    val html =
        remember(svgContent) {
            """
            <!DOCTYPE html>
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
                <style>
                  html, body {
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    margin: 0;
                    padding: 0;
                    background: #F3E7C3;
                    width: 100%;
                    height: calc(var(--vh, 1vh) * 100);
                  }
                  svg {
                    width: 100% !important;
                    height: auto !important;
                    display: block;
                  }
                </style>
                <script>
                  (function() {
                    function setVh() {
                      document.documentElement.style.setProperty('--vh', (window.innerHeight * 0.01) + 'px');
                    }
                    window.addEventListener('resize', setVh);
                    setVh();
                  })();
                </script>
              </head>
              <body>
                $svgContent
              </body>
            </html>
            """.trimIndent()
        }

    val staffShape = RoundedCornerShape(20.dp)
    val borderWidth = if (playbackHighlight) 3.dp else 1.dp
    val borderColor =
        if (playbackHighlight) {
            Color(0xFF38BDF8)
        } else {
            Color.Black.copy(alpha = 0.10f)
        }

    AndroidView(
        factory = { VerovioWebViewPool.acquire() },
        update = { webView ->
            webView.setBackgroundColor(android.graphics.Color.parseColor("#F3E7C3"))
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        onRelease = { webView -> VerovioWebViewPool.release(webView) },
        modifier =
            modifier
                .clip(staffShape)
                .border(width = borderWidth, color = borderColor, shape = staffShape),
    )
}
