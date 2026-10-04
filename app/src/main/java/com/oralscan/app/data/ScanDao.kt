package com.oralscan.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    fun observe(id: Long): Flow<ScanEntity?>

    @Query("SELECT COUNT(*) FROM scans")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Delete
    suspend fun delete(scan: ScanEntity)
}
