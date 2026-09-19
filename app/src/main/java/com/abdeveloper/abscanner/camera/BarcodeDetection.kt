package com.abdeveloper.abscanner.camera

import android.graphics.Rect

data class DetectedBarcode(
    val rawValue: String,
    val formatName: String,
    val boundingBox: Rect? = null,
    val timestamp: Long = System.currentTimeMillis()
)
