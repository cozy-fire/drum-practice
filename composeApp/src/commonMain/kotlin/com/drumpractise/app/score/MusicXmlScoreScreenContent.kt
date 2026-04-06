package com.drumpractise.app.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drumpractise.app.metronome.LocalMetronomeEngine
import com.drumpractise.app.metronome.MetronomeRunConfig
import com.drumpractise.app.metronome.MetronomeSoundPreset
import androidx.compose.foundation.text.KeyboardOptions
import com.drumpractise.app.randompractice.PracticeComposeItem
import com.drumpractise.app.randompractice.RandomPracticeComposer
import drum_practice.composeapp.generated.resources.Res
import drum_practice.composeapp.generated.resources.metronome_note_four_sixteenth_selected
import drum_practice.composeapp.generated.resources.metronome_note_four_sixteenth_unselected
import drum_practice.composeapp.generated.resources.metronome_note_quarter_selected
import drum_practice.composeapp.generated.resources.metronome_note_quarter_unselected
import drum_practice.composeapp.generated.resources.metronome_note_two_eighth_selected
import drum_practice.composeapp.generated.resources.metronome_note_two_eighth_unselected
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicXmlScoreScreenContent(
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

    val zoomSteps = remember { listOf(1.60f, 1.75f, 1.90f, 2.05f, 2.20f, 2.35f, 2.50f, 2.65f, 2.80f, 2.95f) }
    var zoomIndex by remember { mutableIntStateOf(2) } // 1.00f
    val zoomScale = zoomSteps[zoomIndex]

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        IconButton(
                            onClick = { bpm = (bpm - 1).coerceIn(10, 300) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "-1 BPM")
                        }
                        TextButton(
                            onClick = {
                                bpmDraft = ""
                                bpmDialogOpen = true
                            },
                            contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = bpm.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        IconButton(
                            onClick = { bpm = (bpm + 1).coerceIn(10, 300) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "+1 BPM")
                        }

                        Box {
                            TextButton(
                                onClick = { divisorMenuExpanded = true },
                                contentPadding =
                                    androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            ) {
                                Icon(
                                    painter = painterResource(divisorIcon(noteDivisor)),
                                    contentDescription = "分拍",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Unspecified,
                                )
                                Spacer(Modifier.size(1.dp))
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "选择分拍",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = divisorMenuExpanded,
                                onDismissRequest = { divisorMenuExpanded = false },
                            ) {
                                listOf(1, 2, 4).forEach { div ->
                                    DropdownMenuItem(
                                        text = {
                                            Icon(
                                                painter = painterResource(divisorIcon(div)),
                                                contentDescription = divisorLabel(div),
                                                modifier = Modifier.size(18.dp),
                                                tint = Color.Unspecified,
                                            )
                                        },
                                        onClick = {
                                            noteDivisor = div
                                            divisorMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { playing = !playing },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "停止" else "开始",
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TopActionButton(
                    text = "收藏组合",
                    icon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
                    onClick = {},
                    modifier = Modifier.weight(1f).height(44.dp),
                    style = TopActionButtonStyle.Gray,
                )
                TopActionButton(
                    text = "重新生成全部",
                    icon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                    onClick = {
                        scope.launch {
                            selection = RandomPracticeComposer.composeRandom(exclude = selection)
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    style = TopActionButtonStyle.GradientPurpleBlue,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    PracticeCard(
                        eyebrow = "节奏型 RHYTHM PATTERN",
                        title = "布鲁斯洗牌",
                        musicXml = selection?.rhythmicXml.orEmpty(),
                        zoomScale = zoomScale,
                        gradientColors =
                            listOf(
                                Color(0xFF4C2A74),
                                Color(0xFF1D123F),
                            ),
                    )
                }
                item {
                    PracticeCard(
                        eyebrow = "加花 FILL",
                        title = "复合加花",
                        musicXml = selection?.fillXml.orEmpty(),
                        zoomScale = zoomScale,
                        gradientColors =
                            listOf(
                                Color(0xFF123B73),
                                Color(0xFF081A39),
                            ),
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    ZoomBar(
                        zoomPercent = (zoomScale * 100).toInt(),
                        canZoomOut = zoomIndex > 0,
                        canZoomIn = zoomIndex < zoomSteps.lastIndex,
                        onZoomOut = { zoomIndex = (zoomIndex - 1).coerceAtLeast(0) },
                        onZoomIn = { zoomIndex = (zoomIndex + 1).coerceAtMost(zoomSteps.lastIndex) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (bpmDialogOpen) {
        AlertDialog(
            onDismissRequest = { bpmDialogOpen = false },
            title = { Text("BPM (10–300)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("当前：$bpm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = bpmDraft,
                        onValueChange = { bpmDraft = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("BPM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = bpmDraft.toIntOrNull()
                        if (v != null) {
                            bpm = v.coerceIn(10, 300)
                        }
                        bpmDialogOpen = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { bpmDialogOpen = false }) {
                    Text("取消")
                }
            },
        )
    }
}

private fun divisorLabel(noteDivisor: Int): String =
    when (noteDivisor) {
        1 -> "1/4"
        2 -> "1/8"
        4 -> "1/16"
        else -> "1/4"
    }

private fun divisorIcon(noteDivisor: Int): DrawableResource =
    when (noteDivisor) {
        1 -> Res.drawable.metronome_note_quarter_unselected
        2 -> Res.drawable.metronome_note_two_eighth_unselected
        4 -> Res.drawable.metronome_note_four_sixteenth_unselected
        else -> Res.drawable.metronome_note_quarter_unselected
    }

private enum class TopActionButtonStyle {
    Gray,
    GradientPurpleBlue,
}

@Composable
private fun TopActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    style: TopActionButtonStyle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val dark = isSystemInDarkTheme()

    val baseColor: Color
    val baseBrush: Brush?
    val baseAlpha: Float
    val borderAlpha: Float
    val highlightAlpha: Float
    val noiseAlpha: Float

    when (style) {
        TopActionButtonStyle.Gray -> {
            baseBrush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color(0x5E4C4C53).copy(alpha = 0.95f),
                            Color(0x5E4C4C53).copy(alpha = 0.95f),
                        ),
                )
            baseColor = Color.Transparent
            baseAlpha = 0f
            borderAlpha = if (dark) 0.22f else 0.14f
            highlightAlpha = if (dark) 0.14f else 0.10f
            noiseAlpha = if (dark) 0.018f else 0.010f
        }

        TopActionButtonStyle.GradientPurpleBlue -> {
            baseBrush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color(0xFF9B4DFF).copy(alpha = 0.95f),
                            Color(0xFF3B82F6).copy(alpha = 0.95f),
                        ),
                )
            baseColor = Color.Transparent
            baseAlpha = 0f
            borderAlpha = if (dark) 0.22f else 0.14f
            highlightAlpha = if (dark) 0.14f else 0.10f
            noiseAlpha = if (dark) 0.018f else 0.010f
        }
    }

    GlassSurface(
        modifier = modifier,
        shape = shape,
        baseColor = baseColor,
        baseBrush = baseBrush,
        baseAlpha = baseAlpha,
        borderAlpha = borderAlpha,
        highlightAlpha = highlightAlpha,
        noiseAlpha = noiseAlpha,
        onClick = onClick,
        contentPadding = PaddingSpec(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PracticeCard(
    eyebrow: String,
    title: String,
    musicXml: String,
    zoomScale: Float,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    gradientColors.first().copy(alpha = 0.98f),
                                    gradientColors.last().copy(alpha = 0.98f),
                                ),
                        ),
                    )
                    .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    GlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        baseColor = Color.White,
                        baseBrush = null,
                        baseAlpha = 0.10f,
                        borderAlpha = 0.20f,
                        highlightAlpha = 0.12f,
                        noiseAlpha = 0.04f,
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingSpec(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "随机",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF7F7FA))
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(20.dp),
                            ),
                ) {
                    StaffPreview(musicXml = musicXml, zoomScale = zoomScale, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ZoomBar(
    zoomPercent: Int,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onZoomOut, enabled = canZoomOut) {
            Icon(Icons.Filled.Remove, contentDescription = "缩小")
        }
        Text(
            text = "$zoomPercent%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = onZoomIn, enabled = canZoomIn) {
            Icon(Icons.Filled.Add, contentDescription = "放大")
        }
    }
}

private data class PaddingSpec(val horizontal: Dp, val vertical: Dp)

@Composable
private fun GlassSurface(
    shape: Shape,
    baseColor: Color,
    baseBrush: Brush?,
    baseAlpha: Float,
    borderAlpha: Float,
    highlightAlpha: Float,
    noiseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingSpec = PaddingSpec(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val highlight =
        remember(highlightAlpha) {
            Brush.linearGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = highlightAlpha),
                        Color.Transparent,
                    ),
                start = Offset(0f, 0f),
                end = Offset(600f, 400f),
            )
        }

    val noiseSeed = remember { 7_913 }

    Box(
        modifier =
            modifier
                .clip(shape)
                .let { m ->
                    if (baseBrush != null) m.background(baseBrush) else m.background(baseColor.copy(alpha = baseAlpha))
                }
                .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
                .drawWithContent {
                    drawContent()
                    drawRect(brush = highlight, blendMode = BlendMode.SrcOver)

                    val step = 9f
                    val r = 1.2f
                    var y = 0f
                    var row = 0
                    while (y < size.height) {
                        var x = 0f
                        var col = 0
                        while (x < size.width) {
                            val v = ((row * 131 + col * 197 + noiseSeed) % 100) / 100f
                            val a = noiseAlpha * (0.4f + 0.6f * v)
                            drawCircle(
                                color = Color.White.copy(alpha = a),
                                radius = r,
                                center = Offset(x + (v * 3f), y + ((1f - v) * 3f)),
                                style = Fill,
                            )
                            x += step
                            col++
                        }
                        y += step
                        row++
                    }
                }
                .clickable(onClick = onClick)
                .padding(horizontal = contentPadding.horizontal, vertical = contentPadding.vertical),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

