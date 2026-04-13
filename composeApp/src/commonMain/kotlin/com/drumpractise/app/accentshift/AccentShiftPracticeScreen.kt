@file:OptIn(ExperimentalMaterial3Api::class)

package com.drumpractise.app.accentshift

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpractise.app.accentshift.components.AccentShiftPracticeCard
import com.drumpractise.app.accentshift.components.AccentShiftPracticeInfo
import com.drumpractise.app.accentshift.components.AccentShiftPracticeSettingsContent
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.score.StaffZoomStore
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import com.drumpractise.app.score.prefetchStaffPreviewSvgCache
import com.drumpractise.app.accentshift.generator.AccentShiftGenerator
import com.drumpractise.app.accentshift.handmotion.AccentShiftHandMotionPlanner
import com.drumpractise.app.accentshift.handmotion.HandMotionTimeline
import com.drumpractise.app.score.musicxml.MusicXmlRepository
import com.drumpractise.app.score.MusicXmlQueueItem
import com.drumpractise.app.score.ScorePlaybackController
import com.drumpractise.app.score.ScorePlaybackUiState
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.settings.AppSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private data class InstructionSlide(
    val title: String,
    val description: String,
)

private val instructionSlides =
    listOf(
        InstructionSlide("全击", "大幅度挥动，充分利用手腕力量"),
        InstructionSlide("技法说明 2", "占位文案，后续可替换为具体说明。"),
        InstructionSlide("技法说明 3", "占位文案，后续可替换为具体说明。"),
        InstructionSlide("技法说明 4", "占位文案，后续可替换为具体说明。"),
    )

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

    val savedConfig = remember { AppSettings.getAccentShiftPracticeConfig() }
    var config by remember { mutableStateOf(savedConfig) }
    var draftConfig by remember { mutableStateOf(savedConfig) }
    var shuffleNonce by remember {
        mutableIntStateOf(
            if (savedConfig.mode == SeparationPracticeMode.Random) {
                kotlin.random.Random.Default.nextInt()
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
        derivedStateOf { AccentShiftGenerator.generate(config, shuffleNonce = shuffleNonce) }
    }

    LaunchedEffect(trackItems.size) {
        selectedTrackIndex = selectedTrackIndex.coerceIn(0, (trackItems.size - 1).coerceAtLeast(0))
    }

    // 手型 GIF 时间轴（逻辑已就绪，UI 接入时读取 handMotionTimeline）
    @Suppress("UNUSED_VARIABLE")
    var handMotionTimeline by remember { mutableStateOf(HandMotionTimeline(emptyList())) }
    LaunchedEffect(trackItems, config.bpm, config.cardLoopCount) {
        handMotionTimeline =
            AccentShiftHandMotionPlanner.buildHandMotionTimelineForQueue(
                items = trackItems,
                bpm = config.bpm,
                cardLoopCount = config.cardLoopCount,
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

    var isWideLayout by remember { mutableStateOf(false) }

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

    val pagerState = rememberPagerState(pageCount = { instructionSlides.size })

    val hasTracks = trackItems.isNotEmpty()

    fun resetPlayback() {
        ScorePlaybackController.stop()
    }

    fun pausePlayback() {
        ScorePlaybackController.pause()
    }

    fun startPlayback() {
        if (trackItems.isEmpty()) return
        val startIdx =
            when (val s = ScorePlaybackController.uiState.value) {
                is ScorePlaybackUiState.Paused -> s.resumeIndex
                else -> selectedTrackIndex
            }.coerceIn(0, trackItems.lastIndex)
        ScorePlaybackController.playQueue(
            items = trackItems.map { MusicXmlQueueItem(id = it.id, musicXmlPath = it.musicXmlPath) },
            bpm = config.bpm,
            loopCount = config.cardLoopCount.coerceAtLeast(1),
            pcmSampleRate = 48_000,
            startIndex = startIdx,
            weakNoteVolumeScale = ACCENT_SHIFT_WEAK_VOLUME_SCALE,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            resetPlayback()
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
            Spacer(Modifier.height(12.dp))
            InstructionRow(
                pagerState = pagerState,
                slides = instructionSlides,
                onPagerPrev = {
                    scope.launch {
                        val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                        pagerState.animateScrollToPage(target)
                    }
                },
                onPagerNext = {
                    scope.launch {
                        val target = (pagerState.currentPage + 1).coerceAtMost(instructionSlides.lastIndex)
                        pagerState.animateScrollToPage(target)
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            AccentShiftPracticeInfo(
                bpm = config.bpm,
                listLoopCount = config.listLoopCount,
                cardLoopCount = config.cardLoopCount,
                modeLabel = config.mode.label,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                SideEffect {
                    isWideLayout = this.maxWidth >= 600.dp
                }
                val wide = this.maxWidth >= 600.dp
                if (!hasTracks) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "请在右上角设置中勾选练习档位",
                            color = AccentShiftPracticeColors.textMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (!wide) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
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
                        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
                        modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(),
                    ) {
                        AccentShiftPracticeSettingsContent(
                            config = draftConfig,
                            onConfigChange = { draftConfig = it },
                            onClose = { scope.launch { drawerState.close() } },
                            onConfirm = {
                                resetPlayback()
                                config = draftConfig
                                AppSettings.setAccentShiftPracticeConfig(config)
                                if (config.mode == SeparationPracticeMode.Random) shuffleNonce++
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
                            actions = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(end = 4.dp),
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (playing) pausePlayback() else startPlayback()
                                        },
                                        enabled = hasTracks,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (playing) "暂停" else "播放",
                                            tint = AccentShiftPracticeColors.textPrimary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { resetPlayback() },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = "停止",
                                            tint = AccentShiftPracticeColors.textPrimary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            resetPlayback()
                                            draftConfig = config
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "设置",
                                            tint = AccentShiftPracticeColors.textPrimary,
                                        )
                                    }
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
            }
        }
    }
}

@Composable
private fun InstructionRow(
    pagerState: PagerState,
    slides: List<InstructionSlide>,
    onPagerPrev: () -> Unit,
    onPagerNext: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(250.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HandHintBox(label = "L", handContentDescription = "左手提示占位")
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentShiftPracticeColors.surfaceCard),
        ) { page ->
            val slide = slides[page]
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = slide.title,
                    color = AccentShiftPracticeColors.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = AccentShiftPracticeColors.textMuted,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = slide.description,
                    color = AccentShiftPracticeColors.textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = onPagerPrev, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "上一页说明",
                            tint = AccentShiftPracticeColors.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = "${page + 1}/${slides.size}",
                        color = AccentShiftPracticeColors.textMuted,
                        fontSize = 12.sp,
                    )
                    IconButton(onClick = onPagerNext, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "下一页说明",
                            tint = AccentShiftPracticeColors.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    slides.indices.forEach { i ->
                        val dotColor =
                            if (i == page) {
                                AccentShiftPracticeColors.accent
                            } else {
                                AccentShiftPracticeColors.textMuted.copy(alpha = 0.35f)
                            }
                        Box(
                            modifier =
                                Modifier
                                    .size(size = if (i == page) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                        )
                    }
                }
            }
        }
        HandHintBox(label = "R", handContentDescription = "右手提示占位")
    }
}

@Composable
private fun HandHintBox(
    label: String,
    handContentDescription: String,
) {
    Surface(
        modifier =
            Modifier
                .width(36.dp)
                .fillMaxHeight()
                .semantics { contentDescription = handContentDescription }
                .clip(RoundedCornerShape(14.dp)),
        color = AccentShiftPracticeColors.surfaceCard,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                color = AccentShiftPracticeColors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "↑",
                color = AccentShiftPracticeColors.textMuted,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
