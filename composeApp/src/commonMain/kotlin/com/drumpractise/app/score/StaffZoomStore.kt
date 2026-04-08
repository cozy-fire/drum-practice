package com.drumpractise.app.score

import com.drumpractise.app.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global, observable staff zoom source of truth for UI rendering.
 *
 * - previewScale: transient in-memory override (e.g. first-time setup bar +/-)
 * - persistedScale: cached persisted value from AppSettings (since Settings isn't observable)
 *
 * UI should subscribe to [staffZoomScale]. Persist only via [commitScale].
 */
object StaffZoomStore {
    private val previewScale = MutableStateFlow<Float?>(null)
    private val persistedScale = MutableStateFlow(AppSettings.getStaffZoomScale())

    private val staffZoomScaleMutable = MutableStateFlow(previewScale.value ?: persistedScale.value)
    val staffZoomScale: StateFlow<Float> = staffZoomScaleMutable.asStateFlow()

    init {
        // Keep derived flow updated without launching coroutines.
        fun sync() {
            staffZoomScaleMutable.value = previewScale.value ?: persistedScale.value
        }
        previewScale.tryEmit(previewScale.value)
        persistedScale.tryEmit(persistedScale.value)
        sync()
    }

    private fun sync() {
        staffZoomScaleMutable.value = previewScale.value ?: persistedScale.value
    }

    fun setPreviewScale(scale: Float) {
        previewScale.value = scale
        sync()
    }

    fun clearPreview() {
        if (previewScale.value != null) {
            previewScale.value = null
            sync()
        }
    }

    fun commitScale(scale: Float) {
        AppSettings.setStaffZoomScale(scale)
        AppSettings.setStaffZoomConfigured(true)
        persistedScale.value = scale
        previewScale.value = null
        sync()
    }

    /** Optional: force re-read persisted value (rarely needed). */
    fun refreshFromSettings() {
        persistedScale.value = AppSettings.getStaffZoomScale()
        sync()
    }
}

