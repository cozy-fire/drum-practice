package com.drumpractise.app.metronome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpractise.app.theme.DrumAccentBeat
import drum_practice.composeapp.generated.resources.Res
import drum_practice.composeapp.generated.resources.metronome_note_four_sixteenth_selected
import drum_practice.composeapp.generated.resources.metronome_note_four_sixteenth_unselected
import drum_practice.composeapp.generated.resources.metronome_note_quarter_unselected
import drum_practice.composeapp.generated.resources.metronome_note_quarter_selected
import drum_practice.composeapp.generated.resources.metronome_note_two_eighth_selected
import drum_practice.composeapp.generated.resources.metronome_note_two_eighth_unselected
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.atan2
import kotlin.math.roundToInt

private const val BPM_DIAL_COMMIT_DEBOUNCE_MS = 500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = LocalMetronomeEngine.current

    var bpm by remember { mutableIntStateOf(110) }
    /** 圆环拖动时的预览值；null 表示未在拖动预览，界面显示已提交 [bpm]。 */
    var dialPreviewBpm by remember { mutableStateOf<Int?>(null) }
    var commitJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            commitJob?.cancel()
            commitJob = null
        }
    }

    var noteDivisor by remember { mutableIntStateOf(1) }
    var preset by remember { mutableStateOf(MetronomeSoundPreset.Tr707) }
    var playing by remember { mutableStateOf(false) }
    var currentIndexInPeriod by remember { mutableIntStateOf(0) }

    val onBeat: (Int, MetronomeAccent) -> Unit = remember {
        { index, _ -> currentIndexInPeriod = index }
    }

    LaunchedEffect(noteDivisor) {
        currentIndexInPeriod = 0
    }

    LaunchedEffect(playing, bpm, noteDivisor, preset) {
        engine.stop()
        if (!playing) return@LaunchedEffect
        engine.start(MetronomeRunConfig(bpm, noteDivisor, preset), onBeat)
    }

    var bpmDialogOpen by remember { mutableStateOf(false) }
    var bpmDraft by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("节拍器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            BpmDial(
                displayBpm = dialPreviewBpm ?: bpm,
                onDialPreviewChange = { dialPreviewBpm = it.coerceIn(10, 300) },
                onRingDragStart = {
                    commitJob?.cancel()
                    commitJob = null
                },
                onRingDragEnd = {
                    commitJob?.cancel()
                    commitJob =
                        scope.launch {
                            delay(BPM_DIAL_COMMIT_DEBOUNCE_MS)
                            val v = (dialPreviewBpm ?: bpm).coerceIn(10, 300)
                            bpm = v
                            dialPreviewBpm = null
                            commitJob = null
                        }
                },
                onBpmClick = {
                    commitJob?.cancel()
                    commitJob = null
                    dialPreviewBpm = null
                    bpmDraft = bpm.toString()
                    bpmDialogOpen = true
                },
                modifier = Modifier.size(260.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteChoice(
                    noteDivisor = 1,
                    selected = noteDivisor == 1,
                    onClick = { noteDivisor = 1 },
                )
                NoteChoice(
                    noteDivisor = 2,
                    selected = noteDivisor == 2,
                    onClick = { noteDivisor = 2 },
                )
                NoteChoice(
                    noteDivisor = 4,
                    selected = noteDivisor == 4,
                    onClick = { noteDivisor = 4 },
                )
            }
            val period = metronomeBeatPeriod(noteDivisor)
            val dotScroll = rememberScrollState()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(dotScroll),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(period) { index ->
                    val tier = metronomeAccent(index, noteDivisor)
                    val active = currentIndexInPeriod == index
                    val baseStrong = DrumAccentBeat
                    val baseMedium = MaterialTheme.colorScheme.secondary
                    val baseWeak = MaterialTheme.colorScheme.onSurfaceVariant
                    val inactiveStrong = baseStrong.copy(alpha = 0.4f)
                    val inactiveMedium = baseMedium.copy(alpha = 0.4f)
                    val inactiveWeak = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    val color =
                        when (tier) {
                            MetronomeAccent.Strong ->
                                if (active) baseStrong else inactiveStrong
                            MetronomeAccent.Medium ->
                                if (active) baseMedium else inactiveMedium
                            MetronomeAccent.Weak ->
                                if (active) baseWeak else inactiveWeak
                        }
                    Canvas(Modifier.size(20.dp).padding(horizontal = 6.dp)) {
                        drawCircle(color = color, style = Stroke(width = 3.dp.toPx()))
                        if (active) {
                            drawCircle(color = color.copy(alpha = 0.35f), radius = size.minDimension / 4f)
                        }
                    }
                }
            }
            Text("音色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val chipScroll = rememberScrollState()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetronomeSoundPreset.entries.forEach { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = { preset = p },
                        label = { Text(presetLabel(p)) },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            PlayStopButton(
                playing = playing,
                onToggle = { playing = !playing },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    if (bpmDialogOpen) {
        AlertDialog(
            onDismissRequest = { bpmDialogOpen = false },
            title = { Text("BPM (10–300)") },
            text = {
                OutlinedTextField(
                    value = bpmDraft,
                    onValueChange = { bpmDraft = it.filter { ch -> ch.isDigit() }.take(3) },
                    singleLine = true,
                    label = { Text("BPM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        commitJob?.cancel()
                        commitJob = null
                        val v = bpmDraft.toIntOrNull()?.coerceIn(10, 300) ?: bpm
                        bpm = v
                        dialPreviewBpm = null
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

@Composable
private fun presetLabel(p: MetronomeSoundPreset): String =
    when (p) {
        MetronomeSoundPreset.Tr707 -> "TR-707"
    }

@Composable
private fun NoteChoice(
    noteDivisor: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.surfaceVariant
    val icon: DrawableResource =
        when (noteDivisor) {
            1 ->
                if (selected) Res.drawable.metronome_note_quarter_selected
                else Res.drawable.metronome_note_quarter_unselected
            2 ->
                if (selected) Res.drawable.metronome_note_two_eighth_selected
                else Res.drawable.metronome_note_two_eighth_unselected
            4 ->
                if (selected) Res.drawable.metronome_note_four_sixteenth_selected
                else Res.drawable.metronome_note_four_sixteenth_unselected
            else ->
                if (selected) Res.drawable.metronome_note_quarter_selected
                else Res.drawable.metronome_note_quarter_unselected
        }
    Box(
        modifier =
            modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription =
                when (noteDivisor) {
                    1 -> "1个四分音符"
                    2 -> "2个八分音符"
                    4 -> "4个十六分音符"
                    else -> "拍数"
                },
            modifier = Modifier.size(if (noteDivisor == 4)  35.dp else 25.dp),
        )
    }
}

@Composable
private fun BpmDial(
    displayBpm: Int,
    onDialPreviewChange: (Int) -> Unit,
    onRingDragStart: () -> Unit,
    onRingDragEnd: () -> Unit,
    onBpmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onPreview = rememberUpdatedState(onDialPreviewChange)
    val onStart = rememberUpdatedState(onRingDragStart)
    val onEnd = rememberUpdatedState(onRingDragEnd)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onStart.value() },
                    onDragEnd = { onEnd.value() },
                    onDragCancel = { onEnd.value() },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos = change.position
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        deg = (deg + 90f + 360f) % 360f
                        val fraction = deg / 360f
                        onPreview.value((10 + fraction * 290).roundToInt())
                    },
                )
            },
        ) {
            val stroke = 18.dp.toPx()
            val r = size.minDimension / 2f - stroke
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2f, r * 2f)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val sweep = (displayBpm - 10) / 290f * 360f
            drawArc(
                color = secondary,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBpmClick)
                    .padding(16.dp),
        ) {
            Text(
                displayBpm.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = primary,
            )
            Text("BPM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayStopButton(
    playing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                ),
        )
    Box(
        modifier =
            modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(gradient)
                .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "停止" else "播放",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp),
        )
    }
}
