package com.drumpractise.app.settings

import com.russhwolf.settings.Settings

object AppSettings {
    private const val KEY_STAFF_ZOOM_INDEX = "staff_zoom_index"
    private const val KEY_STAFF_ZOOM_CONFIGURED = "staff_zoom_configured"

    private val settings: Settings = Settings()

    fun getStaffZoomIndex(): Int? = settings.getIntOrNull(KEY_STAFF_ZOOM_INDEX)

    fun setStaffZoomIndex(index: Int) {
        settings.putInt(KEY_STAFF_ZOOM_INDEX, index)
    }

    fun getStaffZoomConfigured(): Boolean = settings.getBoolean(KEY_STAFF_ZOOM_CONFIGURED, defaultValue = false)

    fun setStaffZoomConfigured(configured: Boolean) {
        settings.putBoolean(KEY_STAFF_ZOOM_CONFIGURED, configured)
    }
}

