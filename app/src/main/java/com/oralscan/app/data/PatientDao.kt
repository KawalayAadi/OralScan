package com.oralscan.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query(
        """
        SELECT p.*,
            (SELECT COUNT(*) FROM patient_faces f WHERE f.patientId = p.id) AS faceCount,
            (SELECT COUNT(*) FROM scans s WHERE s.patientId = p.id) AS scanCount,
            (SELECT f.imagePath FROM patient_faces f WHERE f.patientId = p.id ORDER BY f.createdAt LIMIT 1) AS avatarPath
        FROM patients p
        ORDER BY p.name COLLATE NOCASE
        """
    )
    fun observeSummaries(): Flow<List<PatientSummary>>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun observe(id: Long): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun get(id: Long): PatientEntity?

    @Query("SELECT * FROM patient_faces WHERE patientId = :patientId ORDER BY createdAt")
    fun observeFaces(patientId: Long): Flow<List<PatientFaceEntity>>

    @Query("SELECT * FROM patient_faces WHERE patientId = :patientId ORDER BY createdAt")
    suspend fun getFaces(patientId: Long): List<PatientFaceEntity>

    @Insert
    suspend fun insert(patient: PatientEntity): Long

    @Delete
    suspend fun delete(patient: PatientEntity)

    @Insert
    suspend fun insertFace(face: PatientFaceEntity): Long

    @Delete
    suspend fun deleteFace(face: PatientFaceEntity)
}
