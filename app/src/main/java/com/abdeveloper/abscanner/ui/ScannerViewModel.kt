package com.abdeveloper.abscanner.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PersistableBundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abdeveloper.abscanner.camera.DetectedBarcode
import com.abdeveloper.abscanner.codec.CheckDigitValidator
import com.abdeveloper.abscanner.codec.CodeParser
import com.abdeveloper.abscanner.codec.CodeType
import com.abdeveloper.abscanner.codec.ParsedCode
import com.abdeveloper.abscanner.codec.RiskLevel
import com.abdeveloper.abscanner.data.AppDatabase
import com.abdeveloper.abscanner.data.ScanItem
import com.abdeveloper.abscanner.data.ScanRepository
import com.abdeveloper.abscanner.data.ScannerPreferences
import com.abdeveloper.abscanner.data.UserSettings
import com.abdeveloper.abscanner.generator.BarcodeFormatMapper
import com.abdeveloper.abscanner.generator.CodeGenerator
import com.abdeveloper.abscanner.generator.GenerationStyle
import com.abdeveloper.abscanner.generator.ModuleShape
import com.abdeveloper.abscanner.generator.ScannabilityVerifier
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import kotlin.coroutines.resume

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository

    init {
        val db = AppDatabase.getInstance(application)
        val prefs = ScannerPreferences(application)
        repository = ScanRepository(db.scanDao(), prefs)
    }

    val historyList: StateFlow<List<ScanItem>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedList: StateFlow<List<ScanItem>> = repository.savedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    // Current Scanned Result View
    private val _currentResult = MutableStateFlow<ParsedCode?>(null)
    val currentResult: StateFlow<ParsedCode?> = _currentResult.asStateFlow()

    private val _currentResultDbId = MutableStateFlow<Long?>(null)
    val currentResultDbId: StateFlow<Long?> = _currentResultDbId.asStateFlow()

    private val _isCurrentSaved = MutableStateFlow(false)
    val isCurrentSaved: StateFlow<Boolean> = _isCurrentSaved.asStateFlow()

    // Camera Controls State
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _hasFlashUnit = MutableStateFlow(true)
    val hasFlashUnit: StateFlow<Boolean> = _hasFlashUnit.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    // Batch Scanning State
    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchScans = MutableStateFlow<List<ScanItem>>(emptyList())
    val batchScans: StateFlow<List<ScanItem>> = _batchScans.asStateFlow()

    // Re-arm logic: a code that stays in view is only reported once; it must leave the
    // camera frame for REARM_GAP_MS before the same value can be reported again.
    private val seenAt = HashMap<String, Long>()

    private var autoOpenJob: Job? = null
    private val _autoOpenCountdown = MutableStateFlow<Int?>(null)
    val autoOpenCountdown: StateFlow<Int?> = _autoOpenCountdown.asStateFlow()

    private companion object {
        const val REARM_GAP_MS = 1500L
    }

    // Gallery Decoding Status
    private val _galleryProcessing = MutableStateFlow(false)
    val galleryProcessing: StateFlow<Boolean> = _galleryProcessing.asStateFlow()

    private val _galleryMessage = MutableStateFlow<String?>(null)
    val galleryMessage: StateFlow<String?> = _galleryMessage.asStateFlow()

    // Generation State
    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _generationVerification = MutableStateFlow<ScannabilityVerifier.VerificationResult?>(null)
    val generationVerification: StateFlow<ScannabilityVerifier.VerificationResult?> = _generationVerification.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun setFlashAvailable(available: Boolean) {
        _hasFlashUnit.value = available
    }

    fun setZoom(zoom: Float) {
        _zoomLevel.value = zoom.coerceIn(1.0f, 10.0f)
    }

    fun toggleBatchMode() {
        _isBatchMode.value = !_isBatchMode.value
    }

    fun clearBatch() {
        _batchScans.value = emptyList()
    }

    fun onBarcodesDetected(list: List<DetectedBarcode>) {
        val now = SystemClock.uptimeMillis()
        // Drop values not seen in this frame if enough time has passed so they can re-trigger.
        val inFrame = list.map { it.rawValue }.toSet()
        val it = seenAt.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (!inFrame.contains(entry.key) && now - entry.value > REARM_GAP_MS) {
                it.remove()
            }
        }
        val firstNew = list.firstOrNull { !seenAt.containsKey(it.rawValue) } ?: return
        seenAt[firstNew.rawValue] = now
        onBarcodeDetected(firstNew, force = false)
    }

    fun onBarcodeDetected(barcode: DetectedBarcode, force: Boolean = false) {
        val nowUptime = SystemClock.uptimeMillis()
        if (!force && seenAt.containsKey(barcode.rawValue) && (nowUptime - (seenAt[barcode.rawValue] ?: 0L)) < REARM_GAP_MS) {
            // Already handled, don't buzz/reopen dialog.
            seenAt[barcode.rawValue] = nowUptime
            return
        }
        seenAt[barcode.rawValue] = nowUptime

        val now = System.currentTimeMillis()
        val parsed = CodeParser.parse(barcode.rawValue, barcode.formatName)

        feedback()

        viewModelScope.launch {
            val item = ScanItem(
                rawValue = barcode.rawValue,
                formatName = barcode.formatName,
                codeType = parsed.type.name,
                title = parsed.title,
                subtitle = parsed.subtitle,
                timestamp = now,
                isSaved = false,
                isCreated = false
            )

            if (_isBatchMode.value) {
                _batchScans.value = listOf(item) + _batchScans.value
                repository.insertScan(item)
            } else {
                val dbId = repository.insertScan(item)
                _currentResultDbId.value = dbId
                _isCurrentSaved.value = false
                _currentResult.value = parsed
                maybeStartAutoOpen(parsed)
            }
        }
    }

    private fun maybeStartAutoOpen(parsed: ParsedCode) {
        cancelAutoOpen()
        val settings = userSettings.value
        if (!settings.autoOpenLinks) return
        if (parsed.type != CodeType.URL) return
        val risk = parsed.urlRisk?.level
        if (risk == RiskLevel.DANGER || risk == RiskLevel.WARNING) return

        autoOpenJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _autoOpenCountdown.value = sec
                delay(1000)
            }
            _autoOpenCountdown.value = null
            executePrimaryAction(parsed)
        }
    }

    fun cancelAutoOpen() {
        autoOpenJob?.cancel()
        autoOpenJob = null
        _autoOpenCountdown.value = null
    }

    private fun feedback() {
        val settings = userSettings.value
        val ctx = getApplication<Application>()
        if (settings.vibrate) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(45)
                    }
                }
            } catch (_: Exception) {}
        }
        if (settings.beep) {
            try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 70).apply {
                    startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                    // ToneGenerator internally plays asynchronously.
                }
            } catch (_: Exception) {}
        }
    }

    fun showResultFor(item: ScanItem) {
        cancelAutoOpen()
        val parsed = CodeParser.parse(item.rawValue, item.formatName)
        _currentResult.value = parsed
        _currentResultDbId.value = item.id
        _isCurrentSaved.value = item.isSaved
    }

    fun dismissResult() {
        cancelAutoOpen()
        _currentResult.value = null
        _currentResultDbId.value = null
        _isCurrentSaved.value = false
    }

    fun toggleCurrentSaved() {
        val id = _currentResultDbId.value ?: return
        val current = _isCurrentSaved.value
        _isCurrentSaved.value = !current
        viewModelScope.launch {
            repository.toggleSaved(id, current)
        }
    }

    fun toggleSavedItem(item: ScanItem) {
        viewModelScope.launch {
            repository.toggleSaved(item.id, item.isSaved)
        }
    }

    fun deleteItem(item: ScanItem) {
        viewModelScope.launch {
            repository.deleteScan(item.id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistoryOnly()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Decode image chosen from Gallery / PhotoPicker
    fun decodeGalleryUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _galleryProcessing.value = true
            _galleryMessage.value = null
            try {
                val context = getApplication<Application>()
                val bitmap = loadBitmapForDecoding(context, uri)
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        _galleryMessage.value = "Failed to load image format."
                        _galleryProcessing.value = false
                    }
                    return@launch
                }

                val detected = decodeBitmapAllPasses(bitmap)
                withContext(Dispatchers.Main) {
                    _galleryProcessing.value = false
                    if (detected != null) {
                        // force = true so gallery scans always open even if the value matches a recent live scan
                        onBarcodeDetected(detected, force = true)
                    } else {
                        _galleryMessage.value = "No code found in this image. Tip: crop closer or try another image."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _galleryMessage.value = "Error reading image: ${e.localizedMessage}"
                    _galleryProcessing.value = false
                }
            }
        }
    }

    private fun loadBitmapForDecoding(context: Context, uri: Uri): Bitmap? {
        val cr = context.contentResolver
        // First pass: read bounds only.
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) } ?: return null
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        // Downsample so max dimension <= 2048 to prevent OOM on 50+ MP phone photos.
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 2048) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) } ?: return null

        // Handle EXIF orientation so rotated photos decode right-side up.
        val rotationDegrees = try {
            cr.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) { 0 }

        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else {
            rawBitmap
        }
    }

    private suspend fun decodeBitmapAllPasses(bitmap: Bitmap): DetectedBarcode? {
        // Pass 1: ML Kit on original bitmap.
        mlKitDecode(bitmap)?.let { return it }

        // Pass 2: ZXing with ALSO_INVERTED on original bitmap.
        zxingDecode(bitmap, inverted = false)?.let { return it }
        zxingDecode(bitmap, inverted = true)?.let { return it }

        // Pass 3: Center-crop (50% scale, center area) for busy photos.
        val cw = (bitmap.width * 0.7f).toInt().coerceAtLeast(1)
        val ch = (bitmap.height * 0.7f).toInt().coerceAtLeast(1)
        val cx = (bitmap.width - cw) / 2
        val cy = (bitmap.height - ch) / 2
        val cropped = Bitmap.createBitmap(bitmap, cx, cy, cw, ch)
        try {
            mlKitDecode(cropped)?.let { return it }
            zxingDecode(cropped, inverted = false)?.let { return it }
        } finally {
            if (cropped != bitmap) cropped.recycle()
        }

        return null
    }

    private suspend fun mlKitDecode(bitmap: Bitmap): DetectedBarcode? =
        suspendCancellableCoroutine { cont ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            val client = BarcodeScanning.getClient(options)
            client.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull()
                    val raw = first?.rawValue ?: first?.displayValue
                    if (first != null && raw != null) {
                        cont.resume(DetectedBarcode(raw, getFormatName(first.format)))
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }

    private fun zxingDecode(bitmap: Bitmap, inverted: Boolean): DetectedBarcode? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val finalSource = if (inverted) source.invert() else source
            val binaryBitmap = BinaryBitmap(HybridBinarizer(finalSource))
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(
                    BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417,
                    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93, BarcodeFormat.CODABAR, BarcodeFormat.ITF
                ))
            }
            val reader = MultiFormatReader().apply { setHints(hints) }
            val result = reader.decodeWithState(binaryBitmap)
            if (result != null && !result.text.isNullOrEmpty()) {
                DetectedBarcode(result.text, BarcodeFormatMapper.displayName(result.barcodeFormat))
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun dismissGalleryMessage() {
        _galleryMessage.value = null
    }

    // Code Generator
    fun generateCode(
        content: String,
        format: BarcodeFormat,
        foregroundColor: Int = AndroidColor.BLACK,
        backgroundColor: Int = AndroidColor.WHITE,
        shape: ModuleShape = ModuleShape.SQUARE,
        ecc: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        logo: Bitmap? = null
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _isGenerating.value = true
            try {
                val style = GenerationStyle(
                    format = format,
                    width = 800,
                    height = 800,
                    foregroundColor = foregroundColor,
                    backgroundColor = backgroundColor,
                    moduleShape = shape,
                    eccLevel = ecc,
                    logoBitmap = logo
                )
                val bmp = CodeGenerator.generate(content, style)
                val verification = ScannabilityVerifier.verify(bmp, content, format)

                withContext(Dispatchers.Main) {
                    _generatedBitmap.value = bmp
                    _generationVerification.value = verification
                    _isGenerating.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _generatedBitmap.value = null
                    _generationVerification.value = ScannabilityVerifier.VerificationResult(
                        isScannable = false,
                        errorMessage = e.localizedMessage ?: "Encoding failed"
                    )
                    _isGenerating.value = false
                }
            }
        }
    }

    fun dismissGenerated() {
        _generatedBitmap.value = null
        _generationVerification.value = null
    }

    fun saveGeneratedToHistory(content: String, format: BarcodeFormat) {
        viewModelScope.launch {
            val formatLabel = BarcodeFormatMapper.displayName(format)
            val parsed = CodeParser.parse(content, formatLabel)
            repository.insertScan(
                ScanItem(
                    rawValue = content,
                    formatName = formatLabel,
                    codeType = parsed.type.name,
                    title = parsed.title,
                    subtitle = parsed.subtitle,
                    timestamp = System.currentTimeMillis(),
                    isSaved = true,
                    isCreated = true
                )
            )
            Toast.makeText(getApplication(), "Saved to Saved Codes", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap, filename: String = "ABScanner_${System.currentTimeMillis()}") {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ABScanner")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("MediaStore insert failed")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ABScanner")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "$filename.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved image to Pictures/ABScanner", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun copyToClipboard(text: String, isSensitive: Boolean = false) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ABScanner", text)
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
        }
        clipboard.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(text: String) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "Share code content").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    fun shareBitmap(bitmap: Bitmap) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
                val file = File(shareDir, "share_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share code image").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to share image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Execute Smart Primary Actions
    fun executePrimaryAction(parsed: ParsedCode) {
        val context = getApplication<Application>()
        try {
            when (parsed.type) {
                CodeType.URL -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parsed.primaryActionIntentUri ?: parsed.rawValue.trim())).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.PHONE -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(parsed.primaryActionIntentUri ?: "tel:${parsed.rawValue}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.SMS -> {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(parsed.primaryActionIntentUri ?: "sms:${parsed.rawValue}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.EMAIL -> {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(parsed.primaryActionIntentUri ?: "mailto:${parsed.rawValue}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.GEO -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parsed.rawValue)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.CONTACT -> {
                    val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                        val nameField = parsed.fields.find { it.label == "Full Name" }?.value
                        val phoneField = parsed.fields.find { it.label == "Phone" }?.value
                        val emailField = parsed.fields.find { it.label == "Email" }?.value
                        nameField?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                        phoneField?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                        emailField?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.CALENDAR -> {
                    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                        val summary = parsed.fields.find { it.label == "Event Title" }?.value ?: parsed.title
                        val location = parsed.fields.find { it.label == "Location" }?.value
                        putExtra(CalendarContract.Events.TITLE, summary)
                        location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.PRODUCT, CodeType.ISBN -> {
                    val uri = parsed.primaryActionIntentUri ?: ("https://www.google.com/search?q=" + Uri.encode(parsed.rawValue))
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                CodeType.WIFI -> {
                    // Open Wi-Fi settings or copy password
                    val pass = parsed.fields.find { it.isSensitive }?.value
                    if (!pass.isNullOrEmpty()) {
                        copyToClipboard(pass, isSensitive = true)
                        Toast.makeText(context, "Wi-Fi password copied! Opening Wi-Fi settings...", Toast.LENGTH_LONG).show()
                    }
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
                else -> {
                    if (parsed.primaryActionIntentUri != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parsed.primaryActionIntentUri)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } else {
                        copyToClipboard(parsed.rawValue)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch action: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Settings Updates
    fun updateTheme(theme: String) = viewModelScope.launch { repository.updateTheme(theme) }
    fun updateLanguage(lang: String) = viewModelScope.launch { repository.updateLanguage(lang) }
    fun updateAutoOpenLinks(enabled: Boolean) = viewModelScope.launch { repository.updateAutoOpenLinks(enabled) }
    fun updateWifiBehavior(behavior: String) = viewModelScope.launch { repository.updateWifiBehavior(behavior) }
    fun updateBeep(enabled: Boolean) = viewModelScope.launch { repository.updateBeep(enabled) }
    fun updateVibrate(enabled: Boolean) = viewModelScope.launch { repository.updateVibrate(enabled) }
    fun updateKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.updateKeepScreenOn(enabled) }
    fun updateScanSpeed(speed: String) = viewModelScope.launch { repository.updateScanSpeed(speed) }
    fun updateSearchEngine(engine: String) = viewModelScope.launch { repository.updateSearchEngine(engine) }
    fun updateStartInBatchMode(enabled: Boolean) = viewModelScope.launch { repository.updateStartInBatchMode(enabled) }
    fun updateFlagSecure(enabled: Boolean) = viewModelScope.launch { repository.updateFlagSecure(enabled) }

    suspend fun exportBackupJson(): String = repository.exportJson(historyList.value)
    suspend fun importBackupJson(json: String): Int = repository.importJson(json)

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
