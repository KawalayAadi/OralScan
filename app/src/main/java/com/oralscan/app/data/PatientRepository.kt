package com.oralscan.app.data

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PatientRepository(
    private val dao: PatientDao,
    private val scanDao: ScanDao,
    private val imageStore: ImageStore,
) {
    fun observeSummaries(): Flow<List<PatientSummary>> = dao.observeSummaries()

    fun observe(id: Long): Flow<PatientEntity?> = dao.observe(id)

    fun observeFaces(patientId: Long): Flow<List<PatientFaceEntity>> = dao.observeFaces(patientId)

    fun observeScans(patientId: Long): Flow<List<ScanEntity>> = scanDao.observeForPatient(patientId)

    suspend fun get(id: Long): PatientEntity? = dao.get(id)

    suspend fun add(name: String, age: Int?, sex: String?, notes: String?): Long =
        dao.insert(
            PatientEntity(
                name = name.trim(),
                age = age,
                sex = sex,
                notes = notes?.trim()?.ifEmpty { null },
                createdAt = System.currentTimeMillis(),
            )
        )

    suspend fun addFace(patientId: Long, face: Bitmap): Long = withContext(Dispatchers.IO) {
        val file = imageStore.saveFace(face)
        dao.insertFace(
            PatientFaceEntity(patientId = patientId, imagePath = file.absolutePath, createdAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteFace(face: PatientFaceEntity) = withContext(Dispatchers.IO) {
        dao.deleteFace(face)
        imageStore.delete(face.imagePath)
    }

    /** Reference photos decoded for verification, in enrollment order. */
    suspend fun loadFaceBitmaps(patientId: Long): List<Bitmap> = withContext(Dispatchers.IO) {
        dao.getFaces(patientId).mapNotNull { imageStore.loadBitmap(it.imagePath) }
    }

    /** Removes the patient, their face photos and all their scans (including files). */
    suspend fun delete(patient: PatientEntity) = withContext(Dispatchers.IO) {
        dao.getFaces(patient.id).forEach { imageStore.delete(it.imagePath) }
        scanDao.getForPatient(patient.id).forEach { scan ->
            scanDao.delete(scan)
            imageStore.delete(scan.imagePath)
            scan.maskPath?.let(imageStore::delete)
        }
        dao.delete(patient) // face rows cascade
    }
}
