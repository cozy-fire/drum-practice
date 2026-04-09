package com.drumpractise.app.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import drumhero.composeapp.generated.resources.Res
import drumhero.composeapp.generated.resources.workbench_metronome
import drumhero.composeapp.generated.resources.workbench_random_practice
import drumhero.composeapp.generated.resources.workbench_separation_practice
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun WorkbenchScreen(
    onOpenMetronome: () -> Unit,
    onOpenMusicXmlScore: () -> Unit,
    onOpenSeparationPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val features =
        listOf(
            WorkbenchFeature(
                title = "节拍器",
                subtitle = "BPM · 音符时值 · 多音色",
                icon = Res.drawable.workbench_metronome,
                gradient = listOf(Color(0xFF5A2E84), Color(0xFF2B1655)),
                onClick = onOpenMetronome,
                iconSize = 60.dp
            ),
            WorkbenchFeature(
                title = "随机练习",
                subtitle = "随机节奏型 · 加花 · 视奏训练",
                icon = Res.drawable.workbench_random_practice,
                gradient = listOf(Color(0xFF224A88), Color(0xFF0F2347)),
                onClick = onOpenMusicXmlScore,
                iconSize = 60.dp
            ),
            WorkbenchFeature(
                title = "手脚分家练习",
                subtitle = "多点位 · 顺序/随机练习",
                icon = Res.drawable.workbench_separation_practice,
                gradient = listOf(Color(0xFF6C3AD8), Color(0xFF1E1340)),
                onClick = onOpenSeparationPractice,
                iconSize = 80.dp
            ),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("首页", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text("选择功能开始您的练习", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(features) { f ->
                WorkbenchFeatureTile(feature = f)
            }
        }
    }
}

private data class WorkbenchFeature(
    val title: String,
    val subtitle: String,
    val icon: DrawableResource,
    val gradient: List<Color>,
    val onClick: () -> Unit,
    val iconSize: Dp
)

@Composable
private fun WorkbenchFeatureTile(
    feature: WorkbenchFeature,
    modifier: Modifier = Modifier,
) {
    val gradientBrush = rememberGradient(feature.gradient)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = feature.onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(gradientBrush)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .padding(10.dp)
                        .size(feature.iconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(feature.icon),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(feature.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(feature.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
                )
        }
    }
}

@Composable
private fun rememberGradient(colors: List<Color>): Brush =
    Brush.linearGradient(colors)
