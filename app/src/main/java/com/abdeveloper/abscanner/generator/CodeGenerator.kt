package com.abdeveloper.abscanner.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

enum class ModuleShape {
    SQUARE,
    ROUNDED,
    DOTS
}

data class GenerationStyle(
    val format: BarcodeFormat = BarcodeFormat.QR_CODE,
    val width: Int = 800,
    val height: Int = 800,
    val foregroundColor: Int = 0xFF000000.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val moduleShape: ModuleShape = ModuleShape.SQUARE,
    val eccLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    val margin: Int = 2,
    val logoBitmap: Bitmap? = null
)

object CodeGenerator {

    fun generate(content: String, style: GenerationStyle): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = style.margin

        // If logo is present, enforce ECC Level H for highest recovery capacity
        val effectiveEcc = if (style.logoBitmap != null) ErrorCorrectionLevel.H else style.eccLevel
        if (style.format == BarcodeFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] = effectiveEcc
        }

        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(content, style.format, style.width, style.height, hints)

        return renderBitMatrix(bitMatrix, style)
    }

    private fun renderBitMatrix(matrix: BitMatrix, style: GenerationStyle): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(style.backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.foregroundColor
            this.style = Paint.Style.FILL
        }

        val isQr = style.format == BarcodeFormat.QR_CODE

        if (!isQr || style.moduleShape == ModuleShape.SQUARE) {
            // Fast standard pixel loop
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (matrix.get(x, y)) {
                        bitmap.setPixel(x, y, style.foregroundColor)
                    }
                }
            }
        } else {
            // Stylized QR drawing (ROUNDED or DOTS)
            // Determine module size
            var moduleWidth = 1f
            for (x in 0 until width) {
                if (matrix.get(x, height / 2)) {
                    // Count consecutive black pixels to estimate module size
                    var count = 0
                    var curX = x
                    while (curX < width && matrix.get(curX, height / 2)) {
                        count++
                        curX++
                    }
                    if (count > 0) {
                        moduleWidth = count.toFloat()
                        break
                    }
                }
            }
            if (moduleWidth <= 0) moduleWidth = (width / 33f).coerceAtLeast(1f)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (matrix.get(x, y)) {
                        when (style.moduleShape) {
                            ModuleShape.DOTS -> {
                                canvas.drawCircle(
                                    x + 0.5f,
                                    y + 0.5f,
                                    0.45f,
                                    paint
                                )
                            }
                            ModuleShape.ROUNDED -> {
                                val rect = RectF(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat())
                                canvas.drawRoundRect(rect, 0.4f, 0.4f, paint)
                            }
                            ModuleShape.SQUARE -> {
                                bitmap.setPixel(x, y, style.foregroundColor)
                            }
                        }
                    }
                }
            }
        }

        // Overlay Logo if present
        style.logoBitmap?.let { logo ->
            val logoSize = (width.coerceAtMost(height) * 0.20f).toInt()
            val left = (width - logoSize) / 2
            val top = (height - logoSize) / 2

            // Draw white badge behind logo
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.backgroundColor
                this.style = Paint.Style.FILL
            }
            val padding = logoSize * 0.08f
            val logoRect = RectF(
                left - padding,
                top - padding,
                left + logoSize + padding,
                top + logoSize + padding
            )
            canvas.drawRoundRect(logoRect, 16f, 16f, bgPaint)

            val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, left.toFloat(), top.toFloat(), null)
        }

        return bitmap
    }
}
