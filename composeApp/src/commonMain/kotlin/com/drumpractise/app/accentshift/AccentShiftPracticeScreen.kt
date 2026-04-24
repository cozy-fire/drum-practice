@file:OptIn(ExperimentalMaterial3Api::class)

package com.drumpractise.app.accentshift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpractise.app.platform.LocalWindowLayoutInfo
import com.drumpractise.app.accentshift.components.AccentShiftHandImageSlot
import com.drumpractise.app.accentshift.components.AccentShiftPracticeCard
import com.drumpractise.app.accentshift.components.AccentShiftPracticeInfo
import com.drumpractise.app.accentshift.components.AccentShiftPracticeSettingsContent
import com.drumpractise.app.accentshift.components.HandImageDisplayMode
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.score.StaffZoomStore
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import com.drumpractise.app.score.prefetchStaffPreviewSvgCache
import com.drumpractise.app.accentshift.generator.AccentShiftGenerator
import com.drumpractise.app.accentshift.handmotion.AccentShiftHandMotionPlanner
import com.drumpractise.app.accentshift.handmotion.HandMotionTimeline
import com.drumpractise.app.score.musicxml.MusicXmlRepository
import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.MusicXmlQueueItem
import com.drumpractise.app.score.ScorePlaybackController
import com.drumpractise.app.score.ScorePlaybackUiState
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.analytics.PracticeAnalytics
import com.drumpractise.app.settings.AppSettings
import com.drumpractise.app.practice.playPracticeCountIn
import com.drumpractise.app.practice.components.PracticeCountInOverlay
import com.drumpractise.app.practice.components.PracticePlaybackFloatingBar
import drumhero.composeapp.generated.resources.Res
import drumhero.composeapp.generated.resources.accent_static_left_up
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random.Default.nextInt

@Composable
fun AccentShiftPracticeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val drawerState =
        remember {
            androidx.compose.material3.DrawerState(
                initialValue = androidx.compose.material3.DrawerValue.Closed,
            )
        }

    val savedState = remember { AppSettings.getAccentShiftPracticeState() }
    var practiceState by remember { mutableStateOf(savedState) }
    var draftState by remember { mutableStateOf(savedState) }
    var shuffleNonce by remember {
        mutableIntStateOf(
            if (savedState.currentParams().mode == SeparationPracticeMode.Random) {
                nextInt()
            } else {
                0
            },
        )
    }

    val zoomSteps = remember { VerovioConfig.ZOOM_STEPS }
    val initialZoomIndex =
        remember {
            val scale = AppSettings.getStaffZoomScale()
            val idx = zoomSteps.indexOfFirst { kotlin.math.abs(it - scale) < 0.0001f }
            (if (idx >= 0) idx else 2).coerceIn(0, zoomSteps.lastIndex)
        }
    var zoomIndex by remember { mutableIntStateOf(initialZoomIndex.coerceIn(0, zoomSteps.lastIndex)) }
    val zoomScale = zoomSteps[zoomIndex]
    var showZoomSetupBar by remember { mutableStateOf(!AppSettings.getStaffZoomConfigured()) }

    LaunchedEffect(showZoomSetupBar, zoomScale) {
        if (showZoomSetupBar) {
            StaffZoomStore.setPreviewScale(zoomScale)
        } else {
            StaffZoomStore.clearPreview()
        }
    }

    var selectedTrackIndex by remember { mutableIntStateOf(0) }

    val trackItems by remember {
        derivedStateOf { AccentShiftGenerator.generate(practiceState, shuffleNonce = shuffleNonce) }
    }

    val activeParams = practiceState.currentParams()

    LaunchedEffect(trackItems) {
        val distinctPaths = trackItems.map { it.musicXmlPath }.distinct()
        for (path in distinctPaths) {
            val xml = MusicXmlRepository.getXml(path)
            if (xml.isBlank()) continue
            withContext(Dispatchers.Default) {
                MusicXmlDrumTimelineParser.parse(xml)
            }
        }
    }

    LaunchedEffect(trackItems.size) {
        selectedTrackIndex = selectedTrackIndex.coerceIn(0, (trackItems.size - 1).coerceAtLeast(0))
    }

    // 手型 GIF 时间轴（后续与 HandImageDisplayMode / 播放进度对齐）
    var handMotionTimeline by remember { mutableStateOf(HandMotionTimeline(emptyList())) }
    LaunchedEffect(trackItems, activeParams.bpm, activeParams.cardLoopCount) {
        handMotionTimeline =
            AccentShiftHandMotionPlanner.buildHandMotionTimelineForQueue(
                items = trackItems,
                bpm = activeParams.bpm,
                cardLoopCount = activeParams.cardLoopCount,
                loadXml = { path -> MusicXmlRepository.getXml(path) },
            )
    }

    val playbackUi by ScorePlaybackController.uiState.collectAsState()
    val playing = playbackUi is ScorePlaybackUiState.Playing

    val playbackLineIndex =
        when (val s = playbackUi) {
            is ScorePlaybackUiState.Playing -> s.index
            is ScorePlaybackUiState.Paused -> s.resumeIndex
            else -> -1
        }

    val staffZoomScale by StaffZoomStore.staffZoomScale.collectAsState()

    val isWideLayout = LocalWindowLayoutInfo.current.isTabletWidth
    val maxWidth = LocalWindowLayoutInfo.current.windowWidth

    val columnState = rememberLazyListState()
    val rowState = rememberLazyListState()

    LaunchedEffect(trackItems, staffZoomScale) {
        val scalePercent =
            (staffZoomScale.coerceIn(0.5f, 2.0f) * 100f).roundToInt().coerceIn(50, 200)
        prefetchStaffPreviewSvgCache(
            paths = trackItems.map { it.musicXmlPath },
            scalePercent = scalePercent,
        )
    }

    val scrollTargetIndex =
        when (val s = playbackUi) {
            is ScorePlaybackUiState.Playing -> s.index
            is ScorePlaybackUiState.Paused -> s.resumeIndex
            else -> selectedTrackIndex
        }

    LaunchedEffect(scrollTargetIndex, trackItems.size, isWideLayout) {
        if (trackItems.isEmpty()) return@LaunchedEffect
        if (scrollTargetIndex >= 0 && scrollTargetIndex < trackItems.size) {
            val targetState = if (isWideLayout) rowState else columnState
            targetState.animateScrollToItem(scrollTargetIndex)
        }
    }

    val hasTracks = trackItems.isNotEmpty()
    val listBottomPad = if (isWideLayout) 200.dp else 100.dp

    var countInBeat by remember { mutableStateOf<Int?>(null) }
    var playbackPrepareJob by remember { mutableStateOf<Job?>(null) }

    fun resetPlayback() {
        playbackPrepareJob?.cancel()
        playbackPrepareJob = null
        countInBeat = null
        ScorePlaybackController.stop()
        selectedTrackIndex = 0
        scope.launch {
            val st = if (isWideLayout) rowState else columnState
            if (trackItems.isNotEmpty()) {
                st.animateScrollToItem(0)
            }
        }
    }

    fun pausePlayback() {
        ScorePlaybackController.pause()
    }

    fun startPlayback() {
        if (trackItems.isEmpty()) return
        playbackPrepareJob?.cancel()
        playbackPrepareJob =
            scope.launch {
                try {
                    val p = practiceState.currentParams()
                    playPracticeCountIn(p.bpm) { beat ->
                        countInBeat = beat
                    }
                } catch (_: CancellationException) {
                    countInBeat = null
                    return@launch
                } finally {
                    countInBeat = null
                }
                val wasPausedBeforePlay = ScorePlaybackController.uiState.value is ScorePlaybackUiState.Paused
                val startIdx =
                    when (val s = ScorePlaybackController.uiState.value) {
                        is ScorePlaybackUiState.Paused -> s.resumeIndex
                        else -> selectedTrackIndex
                    }.coerceIn(0, trackItems.lastIndex)
                val p = practiceState.currentParams()
                ScorePlaybackController.playQueue(
                    items = trackItems.map { MusicXmlQueueItem(id = it.id, musicXmlPath = it.musicXmlPath) },
                    bpm = p.bpm,
                    loopCount = p.cardLoopCount.coerceAtLeast(1),
                    pcmSampleRate = 48_000,
                    startIndex = startIdx,
                    weakNoteVolumeScale = ACCENT_SHIFT_WEAK_VOLUME_SCALE,
                    onQueueFinishedNaturally = { idx ->
                        if (idx == 0 || wasPausedBeforePlay) {
                            PracticeAnalytics.recordAccentShiftPracticeRound(practiceState)
                        }
                        selectedTrackIndex = 0
                        scope.launch {
                            val st = if (isWideLayout) rowState else columnState
                            if (trackItems.isNotEmpty()) {
                                st.animateScrollToItem(0)
                            }
                        }
                    },
                )
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackPrepareJob?.cancel()
            ScorePlaybackController.stop()
        }
    }

    @Composable
    fun MainColumnContent(innerPadding: androidx.compose.foundation.layout.PaddingValues) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            // 手型区域展示暂不实现
            if (false) {
                Spacer(Modifier.height(12.dp))
                InstructionRow(
                    handMotionTimeline = handMotionTimeline,
                    leftHandMode = HandImageDisplayMode.Static(Res.drawable.accent_static_left_up),
                    rightHandMode = HandImageDisplayMode.Static(Res.drawable.accent_static_left_up),
                )
            }
            Spacer(Modifier.height(10.dp))
            AccentShiftPracticeInfo(
                bpm = activeParams.bpm,
                listLoopCount = activeParams.listLoopCount,
                cardLoopCount = activeParams.cardLoopCount,
                modeLabel = activeParams.mode.label,
                selectedAccentTier = practiceState.selectedTier,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            if (!hasTracks) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无练习条目",
                        color = AccentShiftPracticeColors.textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (!isWideLayout) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = listBottomPad),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    state = columnState,
                ) {
                    itemsIndexed(items = trackItems, key = { _, it -> it.id }) { idx, track ->
                        val cardHighlighted =
                            (playbackLineIndex >= 0 && idx == playbackLineIndex) ||
                                    (playbackLineIndex < 0 && idx == selectedTrackIndex)
                        val staffPlaybackHighlight = playing && playbackLineIndex == idx
                        AccentShiftPracticeCard(
                            item = track,
                            cardHighlighted = cardHighlighted,
                            staffPlaybackHighlight = staffPlaybackHighlight,
                            onClick = {
                                selectedTrackIndex = idx
                                if (playing) {
                                    resetPlayback()
                                    startPlayback()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            staffPreviewHeight = 100.dp,
                            contentPadding = 12.dp,
                        )
                    }
                    item(key = "accent_footer_a") {
                        Spacer(Modifier.fillParentMaxHeight(0.48f))
                    }
                    item(key = "accent_footer_b") {
                        Spacer(Modifier.fillParentMaxHeight(0.48f))
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize().padding(bottom = listBottomPad),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    state = rowState,
                ) {
                    itemsIndexed(items = trackItems, key = { _, it -> it.id }) { idx, track ->
                        val cardHighlighted =
                            (playbackLineIndex >= 0 && idx == playbackLineIndex) ||
                                    (playbackLineIndex < 0 && idx == selectedTrackIndex)
                        val staffPlaybackHighlight = playing && playbackLineIndex == idx
                        AccentShiftPracticeCard(
                            item = track,
                            cardHighlighted = cardHighlighted,
                            staffPlaybackHighlight = staffPlaybackHighlight,
                            onClick = {
                                selectedTrackIndex = idx
                                if (playing) {
                                    resetPlayback()
                                    startPlayback()
                                }
                            },
                            modifier =
                                Modifier
                                    .width(maxWidth * 0.4f)
                                    .wrapContentHeight(),
                            staffPreviewHeight = 128.dp,
                            contentPadding = 18.dp,
                        )
                    }
                    item(key = "accent_footer_row_a") {
                        Spacer(Modifier.fillParentMaxWidth(0.48f))
                    }
                    item(key = "accent_footer_row_b") {
                        Spacer(Modifier.fillParentMaxWidth(0.48f))
                    }
                }
            }
        }
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Transparent,
            drawerContent = {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF1B1630),
                        drawerContentColor = Color.White,
                        drawerTonalElevation = 0.dp,
                        drawerShape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
                        modifier = Modifier.fillMaxWidth(if (isWideLayout) 0.3f else 0.7f).fillMaxHeight(),
                    ) {
                        AccentShiftPracticeSettingsContent(
                            practiceState = draftState,
                            onPracticeStateChange = { draftState = it },
                            onClose = { scope.launch { drawerState.close() } },
                            onConfirm = {
                                resetPlayback()
                                practiceState = draftState
                                AppSettings.setAccentShiftPracticeState(practiceState)
                                if (practiceState.currentParams().mode == SeparationPracticeMode.Random) shuffleNonce++
                                scope.launch {
                                    drawerState.close()
                                    val targetState = if (isWideLayout) rowState else columnState
                                    if (trackItems.isNotEmpty()) {
                                        targetState.animateScrollToItem(0)
                                    }
                                }
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                    modifier = modifier,
                    containerColor = AccentShiftPracticeColors.background,
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
                                    canZoomIn = zoomIndex < zoomSteps.lastIndex,
                                    onZoomOut = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) },
                                    onZoomIn = { zoomIndex = (zoomIndex + 1).coerceAtMost(zoomSteps.lastIndex) },
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
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "重音移位",
                                    color = AccentShiftPracticeColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = AccentShiftPracticeColors.textPrimary,
                                    )
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            windowInsets = WindowInsets.statusBars,
                        )
                    },
                ) { innerPadding ->
                    MainColumnContent(innerPadding)
                }
                    PracticePlaybackFloatingBar(
                        playing = playing,
                        playPauseEnabled = hasTracks,
                        onPlayPause = {
                            if (playing) pausePlayback() else startPlayback()
                        },
                        onStop = { resetPlayback() },
                        onSettings = {
                            resetPlayback()
                            draftState = practiceState
                            scope.launch { drawerState.open() }
                        },
                        iconTint = AccentShiftPracticeColors.textPrimary,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .padding(bottom = if (showZoomSetupBar) 200.dp else 0.dp),
                    )
                    countInBeat?.let { beat ->
                        PracticeCountInOverlay(
                            beat1To4 = beat,
                            modifier = Modifier.fillMaxSize(),
                            digitColor = AccentShiftPracticeColors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(
    handMotionTimeline: HandMotionTimeline,
    leftHandMode: HandImageDisplayMode,
    rightHandMode: HandImageDisplayMode,
) {
    key(handMotionTimeline.totalDurationMs, handMotionTimeline.segments.size) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccentShiftHandImageSlot(
                mode = leftHandMode,
                accessibilityDescription = "左手手型",
                modifier =
                    Modifier
                        .weight(1F)
                        .fillMaxHeight(),
            )
            AccentShiftHandImageSlot(
                mode = rightHandMode,
                accessibilityDescription = "右手手型",
                modifier =
                    Modifier
                        .weight(1F)
                        .fillMaxHeight(),
            )
        }
    }
}
