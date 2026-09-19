package com.abdeveloper.abscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "scans")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawValue: String,
    val formatName: String,
    val codeType: String,
    val title: String,
    val subtitle: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val isCreated: Boolean = false
)
