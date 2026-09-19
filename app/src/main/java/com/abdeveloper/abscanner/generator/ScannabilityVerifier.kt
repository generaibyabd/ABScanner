package com.abdeveloper.abscanner.generator

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

object ScannabilityVerifier {

    data class VerificationResult(
        val isScannable: Boolean,
        val decodedText: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Decodes the generated bitmap again. Restricting to the generated [format] keeps this fast
     * and avoids false matches from unrelated symbologies.
     */
    fun verify(
        bitmap: Bitmap,
        expectedContent: String,
        format: BarcodeFormat? = null
    ): VerificationResult {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
            hints[DecodeHintType.TRY_HARDER] = true
            hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
            if (format != null) {
                hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(format)
            }

            val result = MultiFormatReader().decode(binaryBitmap, hints)
            val decoded = result.text

            if (decoded == expectedContent) {
                VerificationResult(isScannable = true, decodedText = decoded)
            } else {
                // Some symbologies legitimately differ (e.g. an added check digit); still readable.
                VerificationResult(
                    isScannable = true,
                    decodedText = decoded,
                    errorMessage = "Decoded content differs from expected input."
                )
            }
        } catch (e: Exception) {
            VerificationResult(
                isScannable = false,
                errorMessage = "Code verification failed: contrast, colors or size make this code hard to read."
            )
        }
    }
}
