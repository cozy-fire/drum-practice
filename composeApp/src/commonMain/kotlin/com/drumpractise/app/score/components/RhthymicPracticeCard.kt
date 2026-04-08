package com.drumpractise.app.score.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drumpractise.app.score.StaffPreview

@Composable
internal fun RhythmicPracticeCard(
    title: String,
    musicXml: String,
    gradientColors: List<Color>,
    onShuffleThis: () -> Unit,
    scorePlaybackActive: Boolean,
    onToggleScorePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            IconButton(onClick = onToggleScorePlayback, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = if (scorePlaybackActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (scorePlaybackActive) "停止谱面播放" else "播放本段谱",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
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
                            IconButton(onClick = onShuffleThis, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = "随机",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF7F7FA)),
                ) {
                    StaffPreview(
                        musicXml = musicXml,
                        playbackHighlight = scorePlaybackActive,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
