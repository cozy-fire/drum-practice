package com.drumpractise.app.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TabletWidthBreakpointStore {
    private val persistedBreakpointDp = MutableStateFlow(AppSettings.getTabletWidthBreakpointDp().dp)

    val breakpointDp: StateFlow<Dp> = persistedBreakpointDp.asStateFlow()

    fun commit(breakpointDp: Dp) {
        val v = breakpointDp.value.coerceIn(320f, 2000f)
        AppSettings.setTabletWidthBreakpointDp(v)
        persistedBreakpointDp.value = v.dp
    }

    /** Optional: force re-read persisted value (rarely needed). */
    fun refreshFromSettings() {
        persistedBreakpointDp.value = AppSettings.getTabletWidthBreakpointDp().dp
    }
}

