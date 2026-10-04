package com.oralscan.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oralscan.app.data.FaceCheck
import com.oralscan.app.data.ScanContext
import com.oralscan.app.ui.analyze.AnalyzeScreen
import com.oralscan.app.ui.camera.CameraScreen
import com.oralscan.app.ui.common.appContainer
import com.oralscan.app.ui.face.FaceEnrollScreen
import com.oralscan.app.ui.face.FaceVerifyScreen
import com.oralscan.app.ui.history.HistoryScreen
import com.oralscan.app.ui.home.HomeScreen
import com.oralscan.app.ui.patients.AddPatientScreen
import com.oralscan.app.ui.patients.PatientDetailScreen
import com.oralscan.app.ui.patients.PatientListScreen
import com.oralscan.app.ui.result.ResultScreen
import com.oralscan.app.ui.review.ReviewScreen
import java.io.File

/**
 * Scan flow:  home -> patients(select) -> face verify -> camera -> review -> analyze -> result
 * The [ScanContext] (patient + face check) rides along as query arguments until the scan is saved.
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"

    const val ARG_PATH = "path"
    const val ARG_SCAN_ID = "scanId"
    const val ARG_PATIENT_ID = "patientId"
    const val ARG_SELECT = "select"
    const val ARG_FOR_SCAN = "forScan"
    private const val ARG_PID = "pid"
    private const val ARG_FC = "fc"
    private const val ARG_FS = "fs"

    private const val CTX = "$ARG_PID={$ARG_PID}&$ARG_FC={$ARG_FC}&$ARG_FS={$ARG_FS}"
    const val CAMERA = "camera?$CTX"
    const val REVIEW = "review?$ARG_PATH={$ARG_PATH}&$CTX"
    const val ANALYZE = "analyze?$ARG_PATH={$ARG_PATH}&$CTX"
    const val RESULT = "result/{$ARG_SCAN_ID}"

    const val PATIENTS = "patients?$ARG_SELECT={$ARG_SELECT}"
    const val PATIENT_NEW = "patient-new?$ARG_FOR_SCAN={$ARG_FOR_SCAN}"
    const val PATIENT_DETAIL = "patient/{$ARG_PATIENT_ID}"
    const val FACE_ENROLL = "patient/{$ARG_PATIENT_ID}/enroll?$ARG_FOR_SCAN={$ARG_FOR_SCAN}"
    const val FACE_VERIFY = "patient/{$ARG_PATIENT_ID}/verify"

    fun camera(ctx: ScanContext) = "camera?${ctx.toQuery()}"
    fun review(path: String, ctx: ScanContext) = "review?$ARG_PATH=${Uri.encode(path)}&${ctx.toQuery()}"
    fun analyze(path: String, ctx: ScanContext) = "analyze?$ARG_PATH=${Uri.encode(path)}&${ctx.toQuery()}"
    fun result(id: Long) = "result/$id"
    fun patients(select: Boolean) = "patients?$ARG_SELECT=$select"
    fun newPatient(forScan: Boolean) = "patient-new?$ARG_FOR_SCAN=$forScan"
    fun patientDetail(id: Long) = "patient/$id"
    fun faceEnroll(id: Long, forScan: Boolean) = "patient/$id/enroll?$ARG_FOR_SCAN=$forScan"
    fun faceVerify(id: Long) = "patient/$id/verify"

    val scanContextArguments = listOf(
        navArgument(ARG_PID) { type = NavType.LongType; defaultValue = -1L },
        navArgument(ARG_FC) { type = NavType.StringType; defaultValue = "" },
        navArgument(ARG_FS) { type = NavType.FloatType; defaultValue = -1f },
    )

    private fun ScanContext.toQuery(): String = buildList {
        patientId?.let { add("$ARG_PID=$it") }
        faceCheck?.let { add("$ARG_FC=${it.name}") }
        faceScore?.let { add("$ARG_FS=$it") }
    }.joinToString("&")

    private fun scanContextOf(pid: Long, fc: String?, fs: Float) = ScanContext(
        patientId = pid.takeIf { it >= 0 },
        faceCheck = fc?.takeIf { it.isNotEmpty() }?.let { runCatching { FaceCheck.valueOf(it) }.getOrNull() },
        faceScore = fs.takeIf { it >= 0f },
    )

    fun scanContextFrom(args: Bundle?): ScanContext =
        scanContextOf(args?.getLong(ARG_PID, -1L) ?: -1L, args?.getString(ARG_FC), args?.getFloat(ARG_FS, -1f) ?: -1f)

    fun scanContextFrom(handle: SavedStateHandle): ScanContext =
        scanContextOf(handle.get<Long>(ARG_PID) ?: -1L, handle.get<String>(ARG_FC), handle.get<Float>(ARG_FS) ?: -1f)
}

private val pathArgument = navArgument(Routes.ARG_PATH) {
    type = NavType.StringType
    defaultValue = ""
}
private val patientIdArgument = navArgument(Routes.ARG_PATIENT_ID) { type = NavType.LongType }
private val forScanArgument = navArgument(Routes.ARG_FOR_SCAN) {
    type = NavType.BoolType
    defaultValue = false
}

@Composable
fun OralScanApp() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewScan = { nav.navigate(Routes.patients(select = true)) },
                onOpenPatients = { nav.navigate(Routes.patients(select = false)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
            )
        }

        // ---- Patients ----
        composable(
            Routes.PATIENTS,
            arguments = listOf(navArgument(Routes.ARG_SELECT) { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val selectMode = entry.arguments?.getBoolean(Routes.ARG_SELECT) ?: false
            PatientListScreen(
                selectMode = selectMode,
                onPatientClick = { id ->
                    nav.navigate(if (selectMode) Routes.faceVerify(id) else Routes.patientDetail(id))
                },
                onAddPatient = { nav.navigate(Routes.newPatient(forScan = selectMode)) },
                onScanWithoutPatient = { nav.navigate(Routes.camera(ScanContext.NONE)) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.PATIENT_NEW, arguments = listOf(forScanArgument)) { entry ->
            val forScan = entry.arguments?.getBoolean(Routes.ARG_FOR_SCAN) ?: false
            AddPatientScreen(
                onSaved = { id ->
                    nav.navigate(Routes.faceEnroll(id, forScan)) {
                        popUpTo(Routes.PATIENT_NEW) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.PATIENT_DETAIL, arguments = listOf(patientIdArgument)) {
            PatientDetailScreen(
                onStartScan = { id -> nav.navigate(Routes.faceVerify(id)) },
                onAddFacePhotos = { id -> nav.navigate(Routes.faceEnroll(id, forScan = false)) },
                onOpenScan = { id -> nav.navigate(Routes.result(id)) },
                onBack = { nav.popBackStack() },
            )
        }

        // ---- Face ----
        composable(Routes.FACE_ENROLL, arguments = listOf(patientIdArgument, forScanArgument)) { entry ->
            val pid = entry.arguments?.getLong(Routes.ARG_PATIENT_ID) ?: -1L
            val forScan = entry.arguments?.getBoolean(Routes.ARG_FOR_SCAN) ?: false
            FaceEnrollScreen(
                onDone = {
                    if (forScan) {
                        // Just enrolled in front of the clinician: no separate verification needed.
                        nav.navigate(Routes.camera(ScanContext(pid, FaceCheck.ENROLLED, null))) {
                            popUpTo(Routes.FACE_ENROLL) { inclusive = true }
                        }
                    } else if (!nav.popBackStack(Routes.PATIENT_DETAIL, inclusive = false)) {
                        // Came from "New patient": replace the enroll screen with the new patient's page.
                        nav.navigate(Routes.patientDetail(pid)) {
                            popUpTo(Routes.FACE_ENROLL) { inclusive = true }
                        }
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.FACE_VERIFY, arguments = listOf(patientIdArgument)) {
            FaceVerifyScreen(
                onContinue = { ctx -> nav.navigate(Routes.camera(ctx)) },
                onAddFacePhotos = { id -> nav.navigate(Routes.faceEnroll(id, forScan = true)) },
                onBack = { nav.popBackStack() },
            )
        }

        // ---- Oral scan ----
        composable(Routes.CAMERA, arguments = Routes.scanContextArguments) { entry ->
            val ctx = Routes.scanContextFrom(entry.arguments)
            CameraScreen(
                patientLabel = patientLabel(ctx),
                onBack = { nav.popBackStack() },
                onPhotoReady = { path -> nav.navigate(Routes.review(path, ctx)) },
            )
        }

        composable(Routes.REVIEW, arguments = listOf(pathArgument) + Routes.scanContextArguments) { entry ->
            val path = entry.arguments?.getString(Routes.ARG_PATH).orEmpty()
            val ctx = Routes.scanContextFrom(entry.arguments)
            ReviewScreen(
                imagePath = path,
                onRetake = {
                    File(path).delete()
                    nav.popBackStack()
                },
                onAnalyze = { nav.navigate(Routes.analyze(path, ctx)) },
            )
        }

        composable(Routes.ANALYZE, arguments = listOf(pathArgument) + Routes.scanContextArguments) {
            AnalyzeScreen(
                onFinished = { scanId ->
                    // Drop the whole scan flow from the back stack: back from the result goes home.
                    nav.navigate(Routes.result(scanId)) { popUpTo(Routes.HOME) }
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            Routes.RESULT,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.LongType }),
        ) {
            ResultScreen(
                onBack = { nav.popBackStack() },
                onNewScan = { nav.navigate(Routes.patients(select = true)) { popUpTo(Routes.HOME) } },
                onOpenPatient = { id -> nav.navigate(Routes.patientDetail(id)) },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenScan = { id -> nav.navigate(Routes.result(id)) },
            )
        }
    }
}

/** "Patient: Jane Doe · Face verified" shown on the oral camera, or null for anonymous scans. */
@Composable
private fun patientLabel(ctx: ScanContext): String? {
    val pid = ctx.patientId ?: return null
    val patients = appContainer().patients
    val name by produceState<String?>(null, pid) { value = patients.get(pid)?.name }
    return listOfNotNull(name?.let { "Patient: $it" }, ctx.faceCheck?.label).joinToString(" · ").ifEmpty { null }
}
