package com.oralscan.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query(
        """
        SELECT s.*, p.name AS patientName FROM scans s
        LEFT JOIN patients p ON p.id = s.patientId
        ORDER BY s.createdAt DESC
        """
    )
    fun observeAllWithPatient(): Flow<List<ScanWithPatient>>

    @Query(
        """
        SELECT s.*, p.name AS patientName FROM scans s
        LEFT JOIN patients p ON p.id = s.patientId
        WHERE s.id = :id
        """
    )
    fun observeWithPatient(id: Long): Flow<ScanWithPatient?>

    @Query("SELECT * FROM scans WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun observeForPatient(patientId: Long): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE patientId = :patientId")
    suspend fun getForPatient(patientId: Long): List<ScanEntity>

    @Query("SELECT COUNT(*) FROM scans")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Delete
    suspend fun delete(scan: ScanEntity)
}
