package com.oralscan.app

import android.app.Application
import android.content.Context
import com.oralscan.app.data.AppDatabase
import com.oralscan.app.data.DevSettings
import com.oralscan.app.data.ImageStore
import com.oralscan.app.data.ScanRepository
import com.oralscan.app.ml.AnalyzerProvider
import com.oralscan.app.ml.LesionAnalyzer

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

    val imageStore = ImageStore(appContext)
    val devSettings = DevSettings(appContext)
    val repository = ScanRepository(AppDatabase.get(appContext).scanDao(), imageStore)

    fun createAnalyzer(): LesionAnalyzer = AnalyzerProvider.create(appContext, devSettings)
}
