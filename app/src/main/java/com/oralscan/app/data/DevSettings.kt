package com.oralscan.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Developer toggles for testing the UI before a real model exists. Persisted across launches. */
class DevSettings(context: Context) {
    private val prefs = context.getSharedPreferences("dev_settings", Context.MODE_PRIVATE)

    private val _fakeMask = MutableStateFlow(prefs.getBoolean(KEY_FAKE_MASK, false))
    val fakeMask: StateFlow<Boolean> = _fakeMask.asStateFlow()

    private val _simulateFailure = MutableStateFlow(prefs.getBoolean(KEY_SIMULATE_FAILURE, false))
    val simulateFailure: StateFlow<Boolean> = _simulateFailure.asStateFlow()

    fun setFakeMask(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAKE_MASK, enabled).apply()
        _fakeMask.value = enabled
    }

    fun setSimulateFailure(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATE_FAILURE, enabled).apply()
        _simulateFailure.value = enabled
    }

    private companion object {
        const val KEY_FAKE_MASK = "fake_mask"
        const val KEY_SIMULATE_FAILURE = "simulate_failure"
    }
}
