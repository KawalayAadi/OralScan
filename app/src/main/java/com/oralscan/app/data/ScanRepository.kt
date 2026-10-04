package com.oralscan.app.data

import android.graphics.Bitmap
import com.oralscan.app.ml.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScanRepository(
    private val dao: ScanDao,
    private val imageStore: ImageStore,
) {
    fun observeAll(): Flow<List<ScanWithPatient>> = dao.observeAllWithPatient()

    fun observe(id: Long): Flow<ScanWithPatient?> = dao.observeWithPatient(id)

    fun observeCount(): Flow<Int> = dao.observeCount()

    /** Stores the (upright) image, optional mask and result. Returns the new scan id. */
    suspend fun save(
        image: Bitmap,
        result: AnalysisResult,
        modelName: String,
        context: ScanContext,
    ): Long = withContext(Dispatchers.IO) {
        val imageFile = imageStore.saveScanImage(image)
        val maskFile = result.mask?.let { imageStore.saveMask(it) }
        dao.insert(
            ScanEntity(
                createdAt = System.currentTimeMillis(),
                imagePath = imageFile.absolutePath,
                imageWidth = image.width,
                imageHeight = image.height,
                maskPath = maskFile?.absolutePath,
                status = result.status.name,
                message = result.message,
                label = result.label,
                confidence = result.confidence,
                modelName = modelName,
                latencyMs = result.latencyMs,
                regionsJson = regionsToJson(result.regions),
                debugJson = debugToJson(result.debug),
                patientId = context.patientId,
                faceCheck = context.faceCheck?.name,
                faceScore = context.faceScore,
            )
        )
    }

    suspend fun delete(scan: ScanEntity) = withContext(Dispatchers.IO) {
        dao.delete(scan)
        imageStore.delete(scan.imagePath)
        scan.maskPath?.let(imageStore::delete)
    }
}
