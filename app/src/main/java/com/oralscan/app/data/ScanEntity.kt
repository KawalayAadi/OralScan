package com.oralscan.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.oralscan.app.ml.AnalysisStatus
import com.oralscan.app.ml.Region
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "scans", indices = [Index("patientId")])
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val maskPath: String?,
    val status: String,
    val message: String,
    val label: String?,
    val confidence: Float?,
    val modelName: String,
    val latencyMs: Long,
    val regionsJson: String,
    val debugJson: String,
    /** Null for scans taken without selecting a patient. */
    val patientId: Long? = null,
    /** [FaceCheck] name. */
    val faceCheck: String? = null,
    /** Fraction of reference photos that matched during verification, 0..1. */
    val faceScore: Float? = null,
)

fun ScanEntity.isSuccess(): Boolean = status == AnalysisStatus.SUCCESS.name

fun ScanEntity.faceCheckValue(): FaceCheck? =
    faceCheck?.let { runCatching { FaceCheck.valueOf(it) }.getOrNull() }

fun ScanEntity.debugEntries(): List<Pair<String, String>> {
    val array = JSONArray(debugJson)
    return List(array.length()) { i ->
        val pair = array.getJSONArray(i)
        pair.getString(0) to pair.getString(1)
    }
}

fun ScanEntity.regions(): List<Region> {
    val array = JSONArray(regionsJson)
    return List(array.length()) { i ->
        val o = array.getJSONObject(i)
        Region(
            left = o.getDouble("l").toFloat(),
            top = o.getDouble("t").toFloat(),
            right = o.getDouble("r").toFloat(),
            bottom = o.getDouble("b").toFloat(),
            label = o.optString("label").ifEmpty { null },
            score = if (o.has("score")) o.getDouble("score").toFloat() else null,
        )
    }
}

internal fun debugToJson(debug: Map<String, String>): String =
    JSONArray().apply { debug.forEach { (k, v) -> put(JSONArray().put(k).put(v)) } }.toString()

internal fun regionsToJson(regions: List<Region>): String =
    JSONArray().apply {
        regions.forEach { r ->
            put(JSONObject().apply {
                put("l", r.left.toDouble())
                put("t", r.top.toDouble())
                put("r", r.right.toDouble())
                put("b", r.bottom.toDouble())
                r.label?.let { put("label", it) }
                r.score?.let { put("score", it.toDouble()) }
            })
        }
    }.toString()
