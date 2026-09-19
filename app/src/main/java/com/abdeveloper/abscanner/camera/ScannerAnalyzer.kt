package com.abdeveloper.abscanner.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

class ScannerAnalyzer(
    private val onBarcodesDetected: (List<DetectedBarcode>) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)
    private var lastScanTime = 0L
    private val minScanIntervalMs = 150L

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private val zxingReader = MultiFormatReader()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < minScanIntervalMs || isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        lastScanTime = currentTime

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val detected = barcodes.mapNotNull { b ->
                        val raw = b.rawValue ?: b.displayValue
                        if (raw != null) {
                            DetectedBarcode(
                                rawValue = raw,
                                formatName = getFormatName(b.format),
                                boundingBox = b.boundingBox
                            )
                        } else null
                    }
                    if (detected.isNotEmpty()) {
                        onBarcodesDetected(detected)
                    }
                } else {
                    // Fallback to ZXing reader on the luminance buffer (e.g. for inverted codes or niche symbologies)
                    tryZxingFallback(imageProxy)
                }
            }
            .addOnFailureListener {
                tryZxingFallback(imageProxy)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun tryZxingFallback(imageProxy: ImageProxy) {
        try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                bytes, width, height, 0, 0, width, height, false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = zxingReader.decodeWithState(binaryBitmap)
            if (result != null && !result.text.isNullOrEmpty()) {
                onBarcodesDetected(
                    listOf(
                        DetectedBarcode(
                            rawValue = result.text,
                            formatName = result.barcodeFormat.name
                        )
                    )
                )
            }
        } catch (_: Exception) {
            // ZXing fallback failed or code not recognized
        } finally {
            zxingReader.reset()
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
