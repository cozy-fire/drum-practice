package com.drumpractise.app.score

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun StaffPreview(
    musicXml: String,
    playbackHighlight: Boolean,
    modifier: Modifier,
) {
    val staffShape = RoundedCornerShape(20.dp)
    val borderWidth = if (playbackHighlight) 3.dp else 1.dp
    val borderColor =
        if (playbackHighlight) Color(0xFF38BDF8) else Color.Black.copy(alpha = 0.10f)
    Box(
        modifier =
            modifier
                .clip(staffShape)
                .border(borderWidth, borderColor, staffShape)
                .background(Color.Transparent),
    )
}
