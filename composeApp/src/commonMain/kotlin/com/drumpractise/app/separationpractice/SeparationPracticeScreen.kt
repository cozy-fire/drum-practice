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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.drumpractise.app.constance.VerovioConfig
import com.drumpractise.app.separationpractice.components.SeparationPracticeCard
import com.drumpractise.app.separationpractice.components.SeparationPracticeInfo
import com.drumpractise.app.separationpractice.components.SeparationPracticeSettingsContent
import com.drumpractise.app.separationpractice.generator.SeparationGenerator
import com.drumpractise.app.separationpractice.model.SeparationConfig
import com.drumpractise.app.separationpractice.model.SeparationPracticeMode
import com.drumpractise.app.score.MusicXmlQueueItem
import com.drumpractise.app.score.ScorePlaybackSession
import com.drumpractise.app.score.ScoreQueuePlayer
import com.drumpractise.app.score.ScoreQueueState
import com.drumpractise.app.score.StaffZoomStore
import com.drumpractise.app.score.prefetchStaffPreviewSvgCache
import com.drumpractise.app.score.rememberScoreHitSoundPlayer
import com.drumpractise.app.score.components.StaffZoomAdjustBar
import com.drumpractise.app.settings.AppSettings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val hitSound = rememberScoreHitSoundPlayer()
    var playing by remember { mutableStateOf(false) }
    var playingIndex by remember { mutableIntStateOf(-1) }
    var pausedResumeIndex by remember { mutableStateOf<Int?>(null) }
    val queuePlayer = remember(hitSound) { ScoreQueuePlayer(hitSound) }
    var session by remember { mutableStateOf<ScorePlaybackSession?>(null) }

    val savedConfig = remember { AppSettings.getSeparationConfig() }
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

    val items by remember {
        derivedStateOf { SeparationGenerator.generate(config, shuffleNonce = shuffleNonce) }
    }

    val staffZoomScale by StaffZoomStore.staffZoomScale.collectAsState()

    var isWideLayout by remember { mutableStateOf(false) }

    val highlightIndex by remember {
        derivedStateOf {
            when {
                playing && playingIndex >= 0 -> playingIndex
                pausedResumeIndex != null -> pausedResumeIndex!!
                else -> -1
            }
        }
    }

    val listState = rememberLazyListState()

    DisposableEffect(hitSound) {
        onDispose {
            session?.stop()
            session = null
            hitSound.stopPlayback()
        }
    }

    LaunchedEffect(hitSound) {
        hitSound.warmup()
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
            listState.animateScrollToItem(highlightIndex)
        }
    }

    LaunchedEffect(session) {
        val s = session ?: run {
            playing = false
            playingIndex = -1
            return@LaunchedEffect
        }
        s.state.collect { st ->
            when (st) {
                is ScoreQueueState.Playing -> {
                    playing = true
                    playingIndex = st.index
                    pausedResumeIndex = null
                }
                ScoreQueueState.Ended -> {
                    playing = false
                    playingIndex = -1
                    pausedResumeIndex = null
                }
                ScoreQueueState.Stopped -> {
                    playing = false
                    playingIndex = -1
                }
                ScoreQueueState.Idle -> {
                    playing = false
                    playingIndex = -1
                }
                is ScoreQueueState.Error -> {
                    playing = false
                    playingIndex = -1
                    pausedResumeIndex = null
                }
            }
        }
    }

    fun resetPlayback() {
        session?.stop()
        session = null
        hitSound.stopPlayback()
        playing = false
        playingIndex = -1
        pausedResumeIndex = null
    }

    fun pausePlayback() {
        val s = session ?: return
        val st = s.state.value
        if (st !is ScoreQueueState.Playing) return
        pausedResumeIndex = st.index
        s.stop()
        session = null
        playing = false
        playingIndex = -1
    }

    fun startPlayback() {
        if (items.isEmpty()) return
        session?.stop()
        session = null
        hitSound.stopPlayback()
        playing = false
        playingIndex = -1
        val startIdx = (pausedResumeIndex ?: 0).coerceIn(0, items.lastIndex)
        scope.launch { hitSound.warmup() }
        session =
            queuePlayer.playQueue(
                items = items.map { MusicXmlQueueItem(id = it.id, musicXmlPath = it.musicXmlPath) },
                bpm = config.bpm,
                loopCount = config.loopCount.coerceAtLeast(1),
                pcmSampleRate = 48_000,
                startIndex = startIdx,
            )
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                        SeparationPracticeSettingsContent(
                            config = draftConfig,
                            onConfigChange = { draftConfig = it },
                            onClose = { scope.launch { drawerState.close() } },
                            onConfirm = {
                                resetPlayback()
                                config = draftConfig
                                AppSettings.setSeparationConfig(config)
                                if (config.mode == SeparationPracticeMode.Random) shuffleNonce++
                                scope.launch {
                                    drawerState.close()
                                    if (!isWideLayout) {
                                        listState.animateScrollToItem(0)
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
                    topBar = {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        resetPlayback()
                                        onBack()
                                    },
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
                                loopCount = config.loopCount,
                                modeLabel = config.mode.label,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                SideEffect {
                                    isWideLayout = maxWidth >= 600.dp
                                }
                                val wide = this.maxWidth >= 600.dp
                                if (!wide) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        state = listState,
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
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items.forEachIndexed { idx, item ->
                                            SeparationPracticeCard(
                                                item = item,
                                                highlighted = highlightIndex == idx,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
