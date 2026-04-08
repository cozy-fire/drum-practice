package com.drumpractise.app.separationpractice.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.drumpractise.app.score.webview.VerovioWebViewPool

@Composable
actual fun StaffPlaceholder(modifier: Modifier) {
    AndroidView(
        factory = { VerovioWebViewPool.acquire() },
        update = { webView ->
            // Blank, no-content WebView placeholder.
            webView.loadDataWithBaseURL(null, "<!DOCTYPE html><html><body></body></html>", "text/html", "UTF-8", null)
        },
        onRelease = { webView -> VerovioWebViewPool.release(webView) },
        modifier = modifier,
    )
}

