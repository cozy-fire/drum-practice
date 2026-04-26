package com.drumpractise.app.separationpractice.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.drumpractise.app.platform.LocalWindowLayoutInfo
import com.drumpractise.app.separationpractice.model.SeparationItem
import com.drumpractise.app.score.StaffPreview
import com.drumpractise.app.score.musicxml.MusicXmlRepository

@Composable
fun SeparationPracticeCard(
    item: SeparationItem,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    var musicXml by remember(item.musicXmlPath) { mutableStateOf("") }
    val isTabletWidth = LocalWindowLayoutInfo.current.isTabletWidth
    val baseStaffPreviewHeight = 128.dp
    val effectiveStaffPreviewHeight = if (isTabletWidth) baseStaffPreviewHeight * 1.5f else baseStaffPreviewHeight

    LaunchedEffect(item.musicXmlPath) {
        musicXml = MusicXmlRepository.getXml(item.musicXmlPath)
    }

    val containerColor = if (highlighted) Color(0xFF3B1B6A) else Color(0xFF2B1655)
    val titleColor = if (highlighted) Color(0xFFFF4DB8) else Color.White

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(effectiveStaffPreviewHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF7F7FA)),
            ) {
                StaffPreview(
                    musicXml = musicXml,
                    playbackHighlight = highlighted,
                    modifier = Modifier.fillMaxWidth().height(effectiveStaffPreviewHeight),
                    staffPreviewCacheKey = item.musicXmlPath,
                )
            }
        }
    }
}

