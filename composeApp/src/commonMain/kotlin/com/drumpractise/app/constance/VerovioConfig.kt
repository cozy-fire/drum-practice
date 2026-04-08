package com.drumpractise.app.constance

object VerovioConfig {
    /** StaffPreview 的缩放步进（来源：原 MusicXmlScoreScreenContent/SettingsScreen 的 zoomSteps）。 */
    val ZOOM_STEPS: List<Float> = List(10) { i -> 0.85f + 0.15f * i }
}