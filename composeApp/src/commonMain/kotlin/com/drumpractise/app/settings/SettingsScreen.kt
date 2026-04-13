package com.drumpractise.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.randompractice.RandomPracticeComposer
import com.drumpractise.app.score.StaffPreview
import com.drumpractise.app.score.StaffZoomStore
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val initialZoomIndex =
        remember {
            val scale = AppSettings.getStaffZoomScale()
            val idx = VerovioConfig.ZOOM_STEPS.indexOfFirst { kotlin.math.abs(it - scale) < 0.0001f }
            (if (idx >= 0) idx else 2).coerceIn(0, VerovioConfig.ZOOM_STEPS.lastIndex)
        }
    var zoomIndex by remember { mutableIntStateOf(initialZoomIndex.coerceIn(0, VerovioConfig.ZOOM_STEPS.lastIndex)) }
    val zoomScale = VerovioConfig.ZOOM_STEPS[zoomIndex]

    var previewXml by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        previewXml = RandomPracticeComposer.composeForSettings().rhythmicXml
    }

    LaunchedEffect(zoomScale) {
        StaffZoomStore.setPreviewScale(zoomScale)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("谱面缩放", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "使用下方 - / + 调整缩放，点击“保存”生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFF7F7FA), RoundedCornerShape(18.dp))
                        .padding(8.dp),
            ) {
                StaffPreview(
                    musicXml = previewXml,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.weight(1f))

            StaffZoomAdjustBar(
                zoomPercent = (zoomScale * 100).toInt(),
                canZoomOut = zoomIndex > 0,
                canZoomIn = zoomIndex < VerovioConfig.ZOOM_STEPS.lastIndex,
                onZoomOut = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) },
                onZoomIn = { zoomIndex = (zoomIndex + 1).coerceAtMost(VerovioConfig.ZOOM_STEPS.lastIndex) },
                confirmText = "保存",
                onConfirm = {
                    StaffZoomStore.commitScale(zoomScale)
                    scope.launch { snackbarHostState.showSnackbar("保存成功") }
                },
            )
        }
    }
}
