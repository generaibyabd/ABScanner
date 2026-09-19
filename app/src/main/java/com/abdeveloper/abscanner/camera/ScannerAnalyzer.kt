package com.abdeveloper.abscanner.camera

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.abdeveloper.abscanner.generator.BarcodeFormatMapper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One long-lived analysis thread for the whole app (never shut down, so late callbacks can't be rejected). */
object ScannerExecutor {
    val instance: Executor = Executors.newSingleThreadExecutor()
}

class ScannerAnalyzer(
    private val callbackExecutor: Executor,
    private val onBarcodesDetected: (List<DetectedBarcode>) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)
    private var lastScanTime = 0L
    private val minScanIntervalMs = 150L
    private var emptyFrames = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
    )

    // ZXing fallback: handles inverted (light-on-dark) codes and symbologies ML Kit misses.
    private val zxingReader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
            BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417,
            BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
            BarcodeFormat.CODABAR, BarcodeFormat.ITF
        )
        hints[DecodeHintType.ALSO_INVERTED] = true
        setHints(hints)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastScanTime < minScanIntervalMs || isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        lastScanTime = now

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // All listeners run on the analysis thread (not the main thread), so the heavier
        // ZXing fallback can never stall the UI.
        scanner.process(inputImage)
            .addOnSuccessListener(callbackExecutor) { barcodes ->
                val detected = barcodes.mapNotNull { b ->
                    val raw = b.rawValue ?: b.displayValue
                    if (raw != null) DetectedBarcode(raw, getFormatName(b.format), b.boundingBox) else null
                }
                if (detected.isNotEmpty()) {
                    emptyFrames = 0
                    deliver(detected)
                } else {
                    emptyFrames++
                    // Run the (more expensive) fallback on every 3rd empty frame only.
                    if (emptyFrames % 3 == 0) tryZxingFallback(imageProxy)
                }
            }
            .addOnFailureListener(callbackExecutor) {
                tryZxingFallback(imageProxy)
            }
            .addOnCompleteListener(callbackExecutor) {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun deliver(list: List<DetectedBarcode>) {
        mainHandler.post { onBarcodesDetected(list) }
    }

    private fun tryZxingFallback(imageProxy: ImageProxy) {
        try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val rowStride = plane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height
            if (rowStride < width || bytes.size < rowStride * (height - 1) + width) return

            // dataWidth = rowStride: the Y plane can be padded, ignoring it skews the image.
            val source = PlanarYUVLuminanceSource(bytes, rowStride, height, 0, 0, width, height, false)
            val result = zxingReader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            if (result != null && !result.text.isNullOrEmpty()) {
                deliver(
                    listOf(
                        DetectedBarcode(
                            rawValue = result.text,
                            formatName = BarcodeFormatMapper.displayName(result.barcodeFormat)
                        )
                    )
                )
            }
        } catch (_: Exception) {
            // Nothing readable in this frame.
        } finally {
            zxingReader.reset()
        }
    }

    fun close() {
        try {
            scanner.close()
        } catch (_: Exception) {
        }
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "Barcode"
        }
    }
}
