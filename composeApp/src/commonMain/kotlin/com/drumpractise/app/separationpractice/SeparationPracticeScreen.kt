package com.drumpractise.app.separationpractice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.platform.LocalWindowLayoutInfo
import com.drumpractise.app.separationpractice.components.SeparationPracticeCard
import com.drumpractise.app.separationpractice.components.SeparationPracticeInfo
import com.drumpractise.app.separationpractice.components.SeparationPracticeSettingsContent
import com.drumpractise.app.separationpractice.generator.SeparationGenerator
import com.drumpractise.app.separationpractice.model.SeparationPracticeLevel
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.score.MusicXmlQueueItem
import com.drumpractise.app.score.ScorePlaybackController
import com.drumpractise.app.score.ScorePlaybackUiState
import com.drumpractise.app.score.StaffZoomStore
import com.drumpractise.app.score.prefetchStaffPreviewSvgCache
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import com.drumpractise.app.score.musicxml.MusicXmlDrumTimelineParser
import com.drumpractise.app.score.musicxml.MusicXmlRepository
import com.drumpractise.app.settings.AppSettings
import com.drumpractise.app.practice.playPracticeCountIn
import com.drumpractise.app.practice.components.PracticeCountInOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeparationPracticeScreen(
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
    val playbackUi by ScorePlaybackController.uiState.collectAsState()
    val playing = playbackUi is ScorePlaybackUiState.Playing

    val initialState = remember { AppSettings.getSeparationPracticeState() }
    var separationState by remember { mutableStateOf(initialState) }
    var config by remember { mutableStateOf(initialState.configForCurrentLevel()) }
    var draftConfig by remember { mutableStateOf(initialState.configForCurrentLevel()) }
    var shuffleNonce by remember {
        mutableIntStateOf(
            if (initialState.configForCurrentLevel().mode == SeparationPracticeMode.Random) {
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

    val items by remember {
        derivedStateOf {
            SeparationGenerator.generate(
                config = config,
                level = separationState.selectedLevel,
                shuffleNonce = shuffleNonce,
            )
        }
    }

    LaunchedEffect(items) {
        val distinctPaths = items.map { it.musicXmlPath }.distinct()
        for (path in distinctPaths) {
            val xml = MusicXmlRepository.getXml(path)
            if (xml.isBlank()) continue
            withContext(Dispatchers.Default) {
                MusicXmlDrumTimelineParser.parse(xml)
            }
        }
    }

    val staffZoomScale by StaffZoomStore.staffZoomScale.collectAsState()

    val isWideLayout = LocalWindowLayoutInfo.current.isTabletWidth
    val maxWidth = LocalWindowLayoutInfo.current.windowWidth

    val highlightIndex =
        when (val s = playbackUi) {
            is ScorePlaybackUiState.Playing -> s.index
            is ScorePlaybackUiState.Paused -> s.resumeIndex
            else -> -1
        }

    val columnState = rememberLazyListState()
    val rowState = rememberLazyListState()

    var countInBeat by remember { mutableStateOf<Int?>(null) }
    var playbackPrepareJob by remember { mutableStateOf<Job?>(null) }

    fun resetPlayback() {
        playbackPrepareJob?.cancel()
        playbackPrepareJob = null
        countInBeat = null
        ScorePlaybackController.stop()
    }

    fun pausePlayback() {
        ScorePlaybackController.pause()
    }

    fun startPlayback() {
        if (items.isEmpty()) return
        playbackPrepareJob?.cancel()
        playbackPrepareJob =
            scope.launch {
                try {
                    playPracticeCountIn(config.bpm) { beat ->
                        countInBeat = beat
                    }
                } catch (_: CancellationException) {
                    countInBeat = null
                    return@launch
                } finally {
                    countInBeat = null
                }
                val startIdx =
                    when (val s = ScorePlaybackController.uiState.value) {
                        is ScorePlaybackUiState.Paused -> s.resumeIndex
                        else -> 0
                    }.coerceIn(0, items.lastIndex)
                ScorePlaybackController.playQueue(
                    items = items.map { MusicXmlQueueItem(id = it.id, musicXmlPath = it.musicXmlPath) },
                    bpm = config.bpm,
                    loopCount = config.cardLoopCount.coerceAtLeast(1),
                    pcmSampleRate = 48_000,
                    startIndex = startIdx,
                )
            }
    }

    fun switchLevel(newLevel: SeparationPracticeLevel) {
        if (newLevel == separationState.selectedLevel) return
        resetPlayback()
        val withCurrentSaved =
            when (separationState.selectedLevel) {
                SeparationPracticeLevel.Basic -> separationState.copy(basicConfig = config)
                SeparationPracticeLevel.Advanced -> separationState.copy(advancedConfig = config)
            }
        val nextState = withCurrentSaved.copy(selectedLevel = newLevel)
        val nextConfig = nextState.configForCurrentLevel()
        separationState = nextState
        config = nextConfig
        draftConfig = nextConfig
        AppSettings.setSeparationPracticeState(nextState)
        if (nextConfig.mode == SeparationPracticeMode.Random) shuffleNonce++
        scope.launch {
            val targetState = if (isWideLayout) rowState else columnState
            targetState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(items, staffZoomScale) {
        val scalePercent =
            (staffZoomScale.coerceIn(0.5f, 2.0f) * 100f).roundToInt().coerceIn(50, 200)
        prefetchStaffPreviewSvgCache(
            paths = items.map { it.musicXmlPath },
            scalePercent = scalePercent,
        )
    }

    LaunchedEffect(highlightIndex, items.size) {
        if (highlightIndex >= 0 && highlightIndex < items.size) {
            val targetState = if (isWideLayout) rowState else columnState
            targetState.animateScrollToItem(highlightIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackPrepareJob?.cancel()
            ScorePlaybackController.stop()
        }
    }

    val rhythmLabel =
        when (separationState.selectedLevel) {
            SeparationPracticeLevel.Basic -> "八分音符点位"
            SeparationPracticeLevel.Advanced -> "十六分音符点位"
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
                        modifier = Modifier.fillMaxWidth(if (isWideLayout) 0.3f else 0.7f).fillMaxHeight(),
                    ) {
                        SeparationPracticeSettingsContent(
                            config = draftConfig,
                            practiceLevel = separationState.selectedLevel,
                            onConfigChange = { draftConfig = it },
                            onClose = { scope.launch { drawerState.close() } },
                            onConfirm = {
                                resetPlayback()
                                config = draftConfig
                                separationState =
                                    when (separationState.selectedLevel) {
                                        SeparationPracticeLevel.Basic -> separationState.copy(basicConfig = config)
                                        SeparationPracticeLevel.Advanced -> separationState.copy(advancedConfig = config)
                                    }
                                AppSettings.setSeparationPracticeState(separationState)
                                if (config.mode == SeparationPracticeMode.Random) shuffleNonce++
                                scope.launch {
                                    drawerState.close()
                                    val targetState = if (isWideLayout) rowState else columnState
                                    targetState.animateScrollToItem(0)
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
                    topBar = {
                        TopAppBar(
                            title = {
                                TabRow(
                                    selectedTabIndex =
                                        if (separationState.selectedLevel == SeparationPracticeLevel.Basic) {
                                            0
                                        } else {
                                            1
                                        },
                                    modifier = Modifier.width(150.dp),
                                ) {
                                    Tab(
                                        selected = separationState.selectedLevel == SeparationPracticeLevel.Basic,
                                        onClick = { switchLevel(SeparationPracticeLevel.Basic) },
                                        text = { Text(SeparationPracticeLevel.Basic.label) },
                                    )
                                    Tab(
                                        selected = separationState.selectedLevel == SeparationPracticeLevel.Advanced,
                                        onClick = { switchLevel(SeparationPracticeLevel.Advanced) },
                                        text = { Text(SeparationPracticeLevel.Advanced.label) },
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBack,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            actions = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(end = 8.dp),
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (playing) pausePlayback() else startPlayback()
                                        },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (playing) "暂停" else "播放",
                                        )
                                    }
                                    IconButton(
                                        onClick = { resetPlayback() },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = "停止",
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
                                        )
                                    }
                                }
                            },
                            windowInsets = WindowInsets.statusBars,
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                ),
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
                ) { innerPadding ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SeparationPracticeInfo(
                                bpm = config.bpm,
                                listLoopCount = config.listLoopCount,
                                cardLoopCount = config.cardLoopCount,
                                modeLabel = config.mode.label,
                                rhythmLabel = rhythmLabel,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "请在右上角设置中勾选练习点位",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    )
                                }
                            } else if (!isWideLayout) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    state = columnState,
                                ) {
                                    itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                                        SeparationPracticeCard(
                                            item = item,
                                            highlighted = highlightIndex == idx,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    state = rowState,
                                ) {
                                    itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                                        SeparationPracticeCard(
                                            item = item,
                                            highlighted = highlightIndex == idx,
                                            modifier = Modifier.width(maxWidth * 0.4f).wrapContentHeight(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                    countInBeat?.let { beat ->
                        PracticeCountInOverlay(
                            beat1To4 = beat,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
