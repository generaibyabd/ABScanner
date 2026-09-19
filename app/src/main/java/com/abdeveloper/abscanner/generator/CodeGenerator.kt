package com.abdeveloper.abscanner.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    /** Quiet zone, in modules (QR spec recommends 4). */
    val margin: Int = 4,
    val logoBitmap: Bitmap? = null
)

object CodeGenerator {

    /** Smallest module size in pixels for styled QR codes, so shapes stay crisp and scannable. */
    private const val MIN_CELL_PX = 12

    private val ONE_D_FORMATS = setOf(
        BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_39, BarcodeFormat.CODE_93, BarcodeFormat.CODE_128,
        BarcodeFormat.CODABAR, BarcodeFormat.ITF
    )

    fun generate(content: String, style: GenerationStyle): Bitmap {
        val format = style.format
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        // Quiet-zone semantics differ per writer: modules for QR/Aztec, pixels for PDF417.
        when {
            format in ONE_D_FORMATS -> hints[EncodeHintType.MARGIN] = 10
            format == BarcodeFormat.PDF_417 -> Unit // keep ZXing's default white space
            else -> hints[EncodeHintType.MARGIN] = style.margin
        }

        val writer = MultiFormatWriter()

        if (format == BarcodeFormat.QR_CODE) {
            // A logo needs maximum error correction to stay scannable.
            hints[EncodeHintType.ERROR_CORRECTION] =
                if (style.logoBitmap != null) ErrorCorrectionLevel.H else style.eccLevel

            // IMPORTANT: encode at 1x1 so ZXing returns the matrix in *module* space
            // (1 cell = 1 module, quiet zone included) instead of pre-scaled to pixels.
            // Styled shapes are drawn per module, never per pixel.
            val matrix = writer.encode(content, format, 1, 1, hints)
            return renderQr(matrix, style)
        }

        val height = if (format in ONE_D_FORMATS) (style.width * 0.375f).toInt().coerceAtLeast(120) else style.height
        val matrix = writer.encode(content, format, style.width, height, hints)
        return renderPlain(matrix, style)
    }

    /** Fast path for barcodes and non-QR 2D codes: straight pixel copy. */
    private fun renderPlain(matrix: BitMatrix, style: GenerationStyle): Bitmap {
        val w = matrix.width
        val h = matrix.height
        val fg = style.foregroundColor
        val bg = style.backgroundColor
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix.get(x, y)) fg else bg
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun renderQr(matrix: BitMatrix, style: GenerationStyle): Bitmap {
        val modules = matrix.width
        val quiet = style.margin.coerceIn(0, 16)
        val dataModules = modules - 2 * quiet

        // Integer pixels per module keeps every edge pixel-aligned (no seams, no blur).
        val cell = maxOf(MIN_CELL_PX, style.width / modules)
        val cellF = cell.toFloat()
        val size = cell * modules

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.backgroundColor)

        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.foregroundColor
            this.style = Paint.Style.FILL
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.backgroundColor
            this.style = Paint.Style.FILL
        }

        fun isDark(x: Int, y: Int): Boolean =
            x in 0 until modules && y in 0 until modules && matrix.get(x, y)

        fun inFinder(x: Int, y: Int): Boolean {
            val lx = x - quiet
            val ly = y - quiet
            val far = dataModules - 7
            return (lx in 0..6 && ly in 0..6) ||
                (lx in far until dataModules && ly in 0..6) ||
                (lx in 0..6 && ly in far until dataModules)
        }

        when (style.moduleShape) {
            ModuleShape.SQUARE -> {
                fgPaint.isAntiAlias = false
                for (y in 0 until modules) {
                    for (x in 0 until modules) {
                        if (matrix.get(x, y)) {
                            canvas.drawRect(x * cellF, y * cellF, (x + 1) * cellF, (y + 1) * cellF, fgPaint)
                        }
                    }
                }
            }

            ModuleShape.DOTS -> {
                val radius = cellF * 0.44f
                for (y in 0 until modules) {
                    for (x in 0 until modules) {
                        if (matrix.get(x, y) && !inFinder(x, y)) {
                            canvas.drawCircle((x + 0.5f) * cellF, (y + 0.5f) * cellF, radius, fgPaint)
                        }
                    }
                }
                drawFinders(canvas, quiet, dataModules, cellF, fgPaint, bgPaint)
            }

            ModuleShape.ROUNDED -> {
                // Round only the outer corners of connected blobs, so neighbours merge smoothly.
                val corner = cellF * 0.45f
                val radii = FloatArray(8)
                val path = Path()
                val rect = RectF()
                for (y in 0 until modules) {
                    for (x in 0 until modules) {
                        if (!matrix.get(x, y) || inFinder(x, y)) continue
                        val up = isDark(x, y - 1)
                        val down = isDark(x, y + 1)
                        val left = isDark(x - 1, y)
                        val right = isDark(x + 1, y)
                        val tl = if (!up && !left) corner else 0f
                        val tr = if (!up && !right) corner else 0f
                        val br = if (!down && !right) corner else 0f
                        val bl = if (!down && !left) corner else 0f
                        radii[0] = tl; radii[1] = tl
                        radii[2] = tr; radii[3] = tr
                        radii[4] = br; radii[5] = br
                        radii[6] = bl; radii[7] = bl
                        rect.set(x * cellF, y * cellF, (x + 1) * cellF, (y + 1) * cellF)
                        path.rewind()
                        path.addRoundRect(rect, radii, Path.Direction.CW)
                        canvas.drawPath(path, fgPaint)
                    }
                }
                drawFinders(canvas, quiet, dataModules, cellF, fgPaint, bgPaint)
            }
        }

        style.logoBitmap?.let { logo -> drawLogo(canvas, size, logo, style.backgroundColor) }

        return bitmap
    }

    /**
     * Finder patterns (the three big "eyes") keep the classic 1:1:3:1:1 geometry with only
     * slightly softened corners. Heavier rounding makes many scanners fail to detect them.
     */
    private fun drawFinders(
        canvas: Canvas,
        quiet: Int,
        dataModules: Int,
        cell: Float,
        fg: Paint,
        bg: Paint
    ) {
        val far = quiet + dataModules - 7
        val origins = listOf(quiet to quiet, far to quiet, quiet to far)
        for ((mx, my) in origins) {
            val l = mx * cell
            val t = my * cell
            canvas.drawRoundRect(RectF(l, t, l + 7 * cell, t + 7 * cell), 0.8f * cell, 0.8f * cell, fg)
            canvas.drawRoundRect(RectF(l + cell, t + cell, l + 6 * cell, t + 6 * cell), 0.4f * cell, 0.4f * cell, bg)
            canvas.drawRoundRect(RectF(l + 2 * cell, t + 2 * cell, l + 5 * cell, t + 5 * cell), 0.4f * cell, 0.4f * cell, fg)
        }
    }

    private fun drawLogo(canvas: Canvas, size: Int, logo: Bitmap, backgroundColor: Int) {
        val logoSize = (size * 0.20f).toInt()
        val left = (size - logoSize) / 2
        val top = (size - logoSize) / 2

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        val padding = logoSize * 0.08f
        val badge = RectF(
            left - padding,
            top - padding,
            left + logoSize + padding,
            top + logoSize + padding
        )
        canvas.drawRoundRect(badge, 16f, 16f, bgPaint)

        val scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
    }
}
