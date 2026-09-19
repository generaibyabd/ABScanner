package com.abdeveloper.abscanner.generator

import com.google.zxing.BarcodeFormat

/**
 * Maps between the names stored in the database / shown to the user
 * ("QR Code", "EAN-13", ...) and ZXing's [BarcodeFormat], accepting both spellings
 * ("QR Code" and "QR_CODE").
 */
object BarcodeFormatMapper {

    private val displayNames = mapOf(
        BarcodeFormat.QR_CODE to "QR Code",
        BarcodeFormat.DATA_MATRIX to "Data Matrix",
        BarcodeFormat.AZTEC to "Aztec",
        BarcodeFormat.PDF_417 to "PDF417",
        BarcodeFormat.EAN_13 to "EAN-13",
        BarcodeFormat.EAN_8 to "EAN-8",
        BarcodeFormat.UPC_A to "UPC-A",
        BarcodeFormat.UPC_E to "UPC-E",
        BarcodeFormat.CODE_128 to "Code 128",
        BarcodeFormat.CODE_39 to "Code 39",
        BarcodeFormat.CODE_93 to "Code 93",
        BarcodeFormat.CODABAR to "Codabar",
        BarcodeFormat.ITF to "ITF"
    )

    fun displayName(format: BarcodeFormat): String = displayNames[format] ?: format.name

    /** Returns a format we are able to *draw*, or null (e.g. unknown or read-only symbologies). */
    fun fromName(name: String): BarcodeFormat? {
        val trimmed = name.trim()
        displayNames.entries.firstOrNull { it.value.equals(trimmed, ignoreCase = true) }?.let { return it.key }
        val normalized = trimmed.uppercase().replace(' ', '_').replace('-', '_')
        val parsed = try {
            BarcodeFormat.valueOf(normalized)
        } catch (_: IllegalArgumentException) {
            null
        }
        return parsed?.takeIf { it in displayNames }
    }
}
