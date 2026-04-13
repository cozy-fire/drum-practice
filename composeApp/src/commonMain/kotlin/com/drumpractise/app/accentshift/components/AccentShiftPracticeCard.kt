package com.drumpractise.app.accentshift.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drumpractise.app.accentshift.AccentShiftPracticeColors
import com.drumpractise.app.accentshift.model.AccentShiftItem
import com.drumpractise.app.score.StaffPreview
import com.drumpractise.app.score.musicxml.MusicXmlRepository

/**
 * @param cardHighlighted Selection / current playback row (border + title).
 * @param staffPlaybackHighlight Passed to [StaffPreview] (beat highlight only while playing).
 */
@Composable
fun AccentShiftPracticeCard(
    item: AccentShiftItem,
    cardHighlighted: Boolean,
    staffPlaybackHighlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    staffPreviewHeight: Dp = 128.dp,
    contentPadding: Dp = 18.dp,
) {
    var musicXml by remember(item.musicXmlPath) { mutableStateOf("") }

    LaunchedEffect(item.musicXmlPath) {
        musicXml = MusicXmlRepository.getXml(item.musicXmlPath)
    }

    val containerColor =
        if (cardHighlighted) Color(0xFF3D2560) else AccentShiftPracticeColors.surfaceCardElevated
    val titleColor =
        if (cardHighlighted) AccentShiftPracticeColors.accent else AccentShiftPracticeColors.textPrimary

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(staffPreviewHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF7F7FA)),
            ) {
                StaffPreview(
                    musicXml = musicXml,
                    playbackHighlight = staffPlaybackHighlight,
                    modifier = Modifier.fillMaxWidth().height(staffPreviewHeight),
                    staffPreviewCacheKey = item.musicXmlPath,
                )
            }
        }
    }
}
