package com.drumpractise.app.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.drumpractise.app.platform.LocalWindowLayoutInfo
import com.drumpractise.app.metronome.LocalMetronomeEngine
import com.drumpractise.app.constance.MetronomeConst
import com.drumpractise.app.metronome.MetronomeRunConfig
import com.drumpractise.app.metronome.MetronomeSoundPreset
import com.drumpractise.app.randompractice.PracticeComposeItem
import com.drumpractise.app.randompractice.RandomPracticeComposer
import com.drumpractise.app.settings.AppSettings
import com.drumpractise.app.score.components.BpmEditDialog
import com.drumpractise.app.score.components.MusicXmlScoreTopBar
import com.drumpractise.app.score.components.RhythmicPracticeCard
import com.drumpractise.app.score.components.ScorePlaybackPart
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import com.drumpractise.app.score.components.TopActionButton
import com.drumpractise.app.score.components.TopActionButtonStyle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

private const val RandomRhythmQueueItemId = "random_rhythm"
private const val RandomFillQueueItemId = "random_fill"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPractiseScreenContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = LocalMetronomeEngine.current
    var bpm by remember { mutableIntStateOf(110) }
    var noteDivisor by remember { mutableIntStateOf(1) }
    val preset = MetronomeSoundPreset.Tr707
    var playing by remember { mutableStateOf(false) }

    var bpmDialogOpen by remember { mutableStateOf(false) }
    var bpmDraft by remember { mutableStateOf("") }

    var divisorMenuExpanded by remember { mutableStateOf(false) }

    var selection by remember { mutableStateOf<PracticeComposeItem?>(null) }
    val scope = rememberCoroutineScope()

    var scorePlaybackPart by remember { mutableStateOf<ScorePlaybackPart?>(null) }
    val scorePlaybackUi by ScorePlaybackController.uiState.collectAsState()
    val rhythmPlaybackActive =
        when (val s = scorePlaybackUi) {
            is ScorePlaybackUiState.Playing -> s.itemId == RandomRhythmQueueItemId
            else -> false
        }
    val fillPlaybackActive =
        when (val s = scorePlaybackUi) {
            is ScorePlaybackUiState.Playing -> s.itemId == RandomFillQueueItemId
            else -> false
        }

    val initialZoomIndex =
        remember {
            val scale = AppSettings.getStaffZoomScale()
            val idx = VerovioConfig.ZOOM_STEPS.indexOfFirst { kotlin.math.abs(it - scale) < 0.0001f }
            (if (idx >= 0) idx else 2).coerceIn(0, VerovioConfig.ZOOM_STEPS.lastIndex)
        }
    var zoomIndex by remember { mutableIntStateOf(initialZoomIndex.coerceIn(0, VerovioConfig.ZOOM_STEPS.lastIndex)) }
    val zoomScale = VerovioConfig.ZOOM_STEPS[zoomIndex]
    var showZoomSetupBar by remember { mutableStateOf(!AppSettings.getStaffZoomConfigured()) }

    LaunchedEffect(showZoomSetupBar, zoomScale) {
        if (showZoomSetupBar) {
            StaffZoomStore.setPreviewScale(zoomScale)
        } else {
            StaffZoomStore.clearPreview()
        }
    }

    LaunchedEffect(Unit) {
        selection = RandomPracticeComposer.composeRandom()
    }

    LaunchedEffect(playing) {
        if (!playing) {
            engine.stop()
        } else {
            engine.start(
                config = MetronomeRunConfig(bpm = bpm, noteDivisor = noteDivisor, preset = preset),
                onBeat = { _, _ -> },
            )
        }
    }

    LaunchedEffect(bpm, noteDivisor) {
        if (playing) {
            engine.updateConfig(MetronomeRunConfig(bpm = bpm, noteDivisor = noteDivisor, preset = preset))
        }
    }

    LaunchedEffect(scorePlaybackPart, bpm, selection?.rhythmicXml, selection?.fillXml) {
        val part = scorePlaybackPart
        if (part == null) {
            ScorePlaybackController.stop()
            return@LaunchedEffect
        }
        val xml =
            when (part) {
                ScorePlaybackPart.Rhythm -> selection?.rhythmicXml
                ScorePlaybackPart.Fill -> selection?.fillXml
            }?.trim().orEmpty()
        if (xml.isEmpty()) {
            ScorePlaybackController.stop()
            return@LaunchedEffect
        }

        val itemId =
            when (part) {
                ScorePlaybackPart.Rhythm -> RandomRhythmQueueItemId
                ScorePlaybackPart.Fill -> RandomFillQueueItemId
            }
        ScorePlaybackController.playQueue(
            items =
                listOf(
                    MusicXmlQueueItem(
                        id = itemId,
                        musicXmlPath = "",
                        inlineMusicXml = xml,
                    ),
                ),
            bpm = bpm,
            loopCount = Int.MAX_VALUE,
            startIndex = 0,
        )
        try {
            awaitCancellation()
        } finally {
            ScorePlaybackController.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
            ScorePlaybackController.stop()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MusicXmlScoreTopBar(
                onBack = onBack,
                bpm = bpm,
                onBpmMinus = { bpm = (bpm - 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX) },
                onBpmPlus = { bpm = (bpm + 1).coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX) },
                onOpenBpmDialog = {
                    bpmDraft = ""
                    bpmDialogOpen = true
                },
                noteDivisor = noteDivisor,
                onNoteDivisorChange = { noteDivisor = it },
                divisorMenuExpanded = divisorMenuExpanded,
                onDivisorMenuExpandedChange = { divisorMenuExpanded = it },
                playing = playing,
                onPlayingToggle = { playing = !playing },
            )
        },
        bottomBar = {
            if (showZoomSetupBar) {
                Box(
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    StaffZoomAdjustBar(
                        zoomPercent = (zoomScale * 100).toInt(),
                        canZoomOut = zoomIndex > 0,
                        canZoomIn = zoomIndex < VerovioConfig.ZOOM_STEPS.lastIndex,
                        onZoomOut = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) },
                        onZoomIn = { zoomIndex = (zoomIndex + 1).coerceAtMost(VerovioConfig.ZOOM_STEPS.lastIndex) },
                        confirmText = "确定",
                        onConfirm = {
                            StaffZoomStore.commitScale(zoomScale)
                            showZoomSetupBar = false
                        },
                        title = "首次设置谱面显示比例",
                        subtitle = "用「− / +」调整五线谱在卡片中的大小，以适配您的屏幕；满意后点「确定」保存，之后可在「设置」中修改。",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TopActionButton(
                    text = "收藏组合",
                    icon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                    onClick = {},
                    modifier = Modifier.wrapContentWidth().height(44.dp),
                    style = TopActionButtonStyle.Gray,
                )
                TopActionButton(
                    text = "随机组合",
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        scope.launch {
                            selection = RandomPracticeComposer.composeRandom(exclude = selection)
                        }
                    },
                    modifier = Modifier.wrapContentWidth().height(44.dp),
                    style = TopActionButtonStyle.GradientPurpleBlue,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                    item {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val wide = LocalWindowLayoutInfo.current.isTabletWidth
                            if (!wide) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    RhythmicPracticeCard(
                                        title = "节奏型",
                                        musicXml = selection?.rhythmicXml.orEmpty(),
                                        gradientColors =
                                            listOf(
                                                Color(0xFF4C2A74),
                                                Color(0xFF1D123F),
                                            ),
                                        onShuffleThis = {
                                            selection?.let { s ->
                                                scope.launch {
                                                    selection = RandomPracticeComposer.composeRandomRhythmOnly(s)
                                                }
                                            }
                                        },
                                        scorePlaybackActive = rhythmPlaybackActive,
                                        onToggleScorePlayback = {
                                            scorePlaybackPart =
                                                if (scorePlaybackPart == ScorePlaybackPart.Rhythm) null else ScorePlaybackPart.Rhythm
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    RhythmicPracticeCard(
                                        title = "加花",
                                        musicXml = selection?.fillXml.orEmpty(),
                                        gradientColors =
                                            listOf(
                                                Color(0xFF123B73),
                                                Color(0xFF081A39),
                                            ),
                                        onShuffleThis = {
                                            selection?.let { s ->
                                                scope.launch {
                                                    selection = RandomPracticeComposer.composeRandomFillOnly(s)
                                                }
                                            }
                                        },
                                        scorePlaybackActive = fillPlaybackActive,
                                        onToggleScorePlayback = {
                                            scorePlaybackPart =
                                                if (scorePlaybackPart == ScorePlaybackPart.Fill) null else ScorePlaybackPart.Fill
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RhythmicPracticeCard(
                                        title = "节奏型",
                                        musicXml = selection?.rhythmicXml.orEmpty(),
                                        gradientColors =
                                            listOf(
                                                Color(0xFF4C2A74),
                                                Color(0xFF1D123F),
                                            ),
                                        onShuffleThis = {
                                            selection?.let { s ->
                                                scope.launch {
                                                    selection = RandomPracticeComposer.composeRandomRhythmOnly(s)
                                                }
                                            }
                                        },
                                        scorePlaybackActive = rhythmPlaybackActive,
                                        onToggleScorePlayback = {
                                            scorePlaybackPart =
                                                if (scorePlaybackPart == ScorePlaybackPart.Rhythm) null else ScorePlaybackPart.Rhythm
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    RhythmicPracticeCard(
                                        title = "加花",
                                        musicXml = selection?.fillXml.orEmpty(),
                                        gradientColors =
                                            listOf(
                                                Color(0xFF123B73),
                                                Color(0xFF081A39),
                                            ),
                                        onShuffleThis = {
                                            selection?.let { s ->
                                                scope.launch {
                                                    selection = RandomPracticeComposer.composeRandomFillOnly(s)
                                                }
                                            }
                                        },
                                        scorePlaybackActive = fillPlaybackActive,
                                        onToggleScorePlayback = {
                                            scorePlaybackPart =
                                                if (scorePlaybackPart == ScorePlaybackPart.Fill) null else ScorePlaybackPart.Fill
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }

    BpmEditDialog(
        open = bpmDialogOpen,
        currentBpm = bpm,
        bpmDraft = bpmDraft,
        onBpmDraftChange = { bpmDraft = it },
        onDismiss = { bpmDialogOpen = false },
        onConfirm = { v ->
            if (v != null) {
                bpm = v.coerceIn(MetronomeConst.BPM_MIN, MetronomeConst.BPM_MAX)
            }
        },
    )
}
