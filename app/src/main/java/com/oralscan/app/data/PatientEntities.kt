package com.oralscan.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int?,
    val sex: String?,
    val notes: String?,
    val createdAt: Long,
)

/** A reference face photo used to verify the patient (the Kivy app's "verification_images"). */
@Entity(
    tableName = "patient_faces",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patientId")],
)
data class PatientFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val imagePath: String,
    val createdAt: Long,
)

data class PatientSummary(
    @Embedded val patient: PatientEntity,
    val faceCount: Int,
    val scanCount: Int,
    val avatarPath: String?,
)

data class ScanWithPatient(
    @Embedded val scan: ScanEntity,
    val patientName: String?,
)

fun PatientEntity.subtitle(): String =
    listOfNotNull(age?.let { "Age $it" }, sex).joinToString(" · ")
