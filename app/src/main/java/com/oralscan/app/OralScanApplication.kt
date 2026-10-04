package com.oralscan.app

import android.app.Application
import android.content.Context
import com.oralscan.app.data.AppDatabase
import com.oralscan.app.data.DevSettings
import com.oralscan.app.data.ImageStore
import com.oralscan.app.data.PatientRepository
import com.oralscan.app.data.ScanRepository
import com.oralscan.app.ml.AnalyzerProvider
import com.oralscan.app.ml.LesionAnalyzer
import com.oralscan.app.ml.face.FaceVerifier
import com.oralscan.app.ml.face.FaceVerifierProvider

class OralScanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-rolled dependency container: the app is small enough not to need Hilt. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)

    val imageStore = ImageStore(appContext)
    val devSettings = DevSettings(appContext)
    val repository = ScanRepository(database.scanDao(), imageStore)
    val patients = PatientRepository(database.patientDao(), database.scanDao(), imageStore)

    /** Loaded on first use (the real model is large); first access should happen off the main thread. */
    val faceVerifier: FaceVerifier by lazy { FaceVerifierProvider.create(appContext) }

    fun createAnalyzer(): LesionAnalyzer = AnalyzerProvider.create(appContext, devSettings)
}
