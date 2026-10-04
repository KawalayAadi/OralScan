package com.oralscan.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Developer toggles and tuning values for testing. Persisted across launches. */
class DevSettings(context: Context) {
    private val prefs = context.getSharedPreferences("dev_settings", Context.MODE_PRIVATE)

    private val _fakeMask = MutableStateFlow(prefs.getBoolean(KEY_FAKE_MASK, false))
    val fakeMask: StateFlow<Boolean> = _fakeMask.asStateFlow()

    private val _simulateFailure = MutableStateFlow(prefs.getBoolean(KEY_SIMULATE_FAILURE, false))
    val simulateFailure: StateFlow<Boolean> = _simulateFailure.asStateFlow()

    private val _faceDetectionThreshold =
        MutableStateFlow(prefs.getFloat(KEY_FACE_DETECTION, DEFAULT_FACE_DETECTION_THRESHOLD))
    /** A reference photo "matches" when the model score is above this (faceid.py: detection_threshold). */
    val faceDetectionThreshold: StateFlow<Float> = _faceDetectionThreshold.asStateFlow()

    private val _faceVerificationThreshold =
        MutableStateFlow(prefs.getFloat(KEY_FACE_VERIFICATION, DEFAULT_FACE_VERIFICATION_THRESHOLD))
    /** Verified when the fraction of matching references is above this (faceid.py: verification_threshold). */
    val faceVerificationThreshold: StateFlow<Float> = _faceVerificationThreshold.asStateFlow()

    fun setFakeMask(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAKE_MASK, enabled).apply()
        _fakeMask.value = enabled
    }

    fun setSimulateFailure(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATE_FAILURE, enabled).apply()
        _simulateFailure.value = enabled
    }

    fun setFaceDetectionThreshold(value: Float) {
        prefs.edit().putFloat(KEY_FACE_DETECTION, value).apply()
        _faceDetectionThreshold.value = value
    }

    fun setFaceVerificationThreshold(value: Float) {
        prefs.edit().putFloat(KEY_FACE_VERIFICATION, value).apply()
        _faceVerificationThreshold.value = value
    }

    fun resetFaceThresholds() {
        setFaceDetectionThreshold(DEFAULT_FACE_DETECTION_THRESHOLD)
        setFaceVerificationThreshold(DEFAULT_FACE_VERIFICATION_THRESHOLD)
    }

    companion object {
        // Same defaults as faceid.py in Face_Recognition_App.
        const val DEFAULT_FACE_DETECTION_THRESHOLD = 0.2f
        const val DEFAULT_FACE_VERIFICATION_THRESHOLD = 0.2f

        private const val KEY_FAKE_MASK = "fake_mask"
        private const val KEY_SIMULATE_FAILURE = "simulate_failure"
        private const val KEY_FACE_DETECTION = "face_detection_threshold"
        private const val KEY_FACE_VERIFICATION = "face_verification_threshold"
    }
}
