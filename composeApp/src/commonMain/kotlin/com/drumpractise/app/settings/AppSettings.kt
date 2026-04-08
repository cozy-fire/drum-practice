package com.drumpractise.app.settings

import com.drumpractise.app.constance.VerovioConfig
import com.russhwolf.settings.Settings

object AppSettings {
    private const val KEY_STAFF_ZOOM_INDEX = "staff_zoom_index"
    private const val KEY_STAFF_ZOOM_CONFIGURED = "staff_zoom_configured"
    private const val KEY_STAFF_ZOOM_SCALE = "staff_zoom_scale"

    private val settings: Settings = Settings()

    fun getStaffZoomIndex(): Int? = settings.getIntOrNull(KEY_STAFF_ZOOM_INDEX)

    fun setStaffZoomIndex(index: Int) {
        settings.putInt(KEY_STAFF_ZOOM_INDEX, index)
    }

    fun getStaffZoomScale(): Float {
        settings.getFloatOrNull(KEY_STAFF_ZOOM_SCALE)?.let { return it }

        // 兼容迁移：旧版本仅保存 index。
        val index = getStaffZoomIndex()
        val fallback =
            if (index == null) {
                VerovioConfig.ZOOM_STEPS.getOrElse(2) { 0.85f }
            } else {
                VerovioConfig.ZOOM_STEPS.getOrElse(index) { VerovioConfig.ZOOM_STEPS.getOrElse(2) { 0.85f } }
            }
        settings.putFloat(KEY_STAFF_ZOOM_SCALE, fallback)
        return fallback
    }

    fun setStaffZoomScale(scale: Float) {
        settings.putFloat(KEY_STAFF_ZOOM_SCALE, scale)
    }

    fun getStaffZoomConfigured(): Boolean = settings.getBoolean(KEY_STAFF_ZOOM_CONFIGURED, defaultValue = false)

    fun setStaffZoomConfigured(configured: Boolean) {
        settings.putBoolean(KEY_STAFF_ZOOM_CONFIGURED, configured)
    }
}

