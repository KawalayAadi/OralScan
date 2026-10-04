package com.oralscan.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oralscan.app.ui.analyze.AnalyzeScreen
import com.oralscan.app.ui.camera.CameraScreen
import com.oralscan.app.ui.history.HistoryScreen
import com.oralscan.app.ui.home.HomeScreen
import com.oralscan.app.ui.result.ResultScreen
import com.oralscan.app.ui.review.ReviewScreen
import java.io.File

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val HISTORY = "history"
    const val ARG_PATH = "path"
    const val ARG_SCAN_ID = "scanId"
    const val REVIEW = "review?$ARG_PATH={$ARG_PATH}"
    const val ANALYZE = "analyze?$ARG_PATH={$ARG_PATH}"
    const val RESULT = "result/{$ARG_SCAN_ID}"

    fun review(path: String) = "review?$ARG_PATH=${Uri.encode(path)}"
    fun analyze(path: String) = "analyze?$ARG_PATH=${Uri.encode(path)}"
    fun result(id: Long) = "result/$id"
}

private val pathArgument = listOf(navArgument(Routes.ARG_PATH) {
    type = NavType.StringType
    defaultValue = ""
})

@Composable
fun OralScanApp() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onTakePhoto = { nav.navigate(Routes.CAMERA) },
                onPhotoImported = { path -> nav.navigate(Routes.review(path)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onBack = { nav.popBackStack() },
                onPhotoCaptured = { path -> nav.navigate(Routes.review(path)) },
            )
        }

        composable(Routes.REVIEW, arguments = pathArgument) { entry ->
            val path = entry.arguments?.getString(Routes.ARG_PATH).orEmpty()
            ReviewScreen(
                imagePath = path,
                onRetake = {
                    File(path).delete()
                    nav.popBackStack()
                },
                onAnalyze = { nav.navigate(Routes.analyze(path)) },
            )
        }

        composable(Routes.ANALYZE, arguments = pathArgument) {
            AnalyzeScreen(
                onFinished = { scanId ->
                    // Drop camera/review/analyze from the back stack: back from the result goes home.
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
                onNewScan = { nav.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) } },
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
