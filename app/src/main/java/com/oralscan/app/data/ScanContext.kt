package com.oralscan.app.data

/** How the patient's identity was confirmed before the oral photo was taken. */
enum class FaceCheck(val label: String) {
    VERIFIED("Face verified"),
    NOT_VERIFIED("Face NOT verified"),
    SKIPPED("Verification skipped"),
    ENROLLED("Enrolled during this visit"),
}

/** Carried from patient selection through camera -> review -> analyze, then saved with the scan. */
data class ScanContext(
    val patientId: Long? = null,
    val faceCheck: FaceCheck? = null,
    val faceScore: Float? = null,
) {
    companion object {
        val NONE = ScanContext()
    }
}
