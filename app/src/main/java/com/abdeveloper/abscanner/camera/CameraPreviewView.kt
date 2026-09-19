package com.abdeveloper.abscanner.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    zoomLevel: Float = 1.0f,
    detectedBarcodes: List<DetectedBarcode> = emptyList(),
    onBarcodesDetected: (List<DetectedBarcode>) -> Unit,
    onBarcodeSelected: (DetectedBarcode) -> Unit,
    onHasFlashUnitChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var currentZoom by remember { mutableFloatStateOf(zoomLevel) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(zoomLevel, camera) {
        camera?.cameraControl?.setZoomRatio(zoomLevel)
        currentZoom = zoomLevel
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (currentZoom * zoom).coerceIn(1.0f, 10.0f)
                        currentZoom = newZoom
                        camera?.cameraControl?.setZoomRatio(newZoom)
                    }
                },
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, ScannerAnalyzer(onBarcodesDetected))
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        camera = cam
                        onHasFlashUnitChanged(cam.cameraInfo.hasFlashUnit())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                        v.performClick()
                    }
                    true
                }

                previewView
            }
        )

        // Viewfinder overlay
        ViewfinderOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // If multiple barcodes are detected, show interactive indicator tags
        if (detectedBarcodes.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "Multiple codes detected: Tap one to view",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ViewfinderOverlay(
    modifier: Modifier = Modifier,
    scannerColor: Color = Color(0xFF3785D2)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Sizing the viewfinder box in the middle
        val boxSide = (minOf(width, height) * 0.72f).coerceAtLeast(260.dp.toPx())
        val left = (width - boxSide) / 2f
        val top = (height - boxSide) / 2.3f
        val right = left + boxSide
        val bottom = top + boxSide

        // Draw translucent dark scrim outside the viewfinder
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            size = Size(width, height)
        )

        // Cut out the clear transparent center
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Draw subtle inner guide border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw corner brackets in brand accent color
        val cornerLength = 32.dp.toPx()
        val strokeWidth = 4.5.dp.toPx()
        val cornerRadiusPx = 20.dp.toPx()

        // Top-Left
        val pathTL = Path().apply {
            moveTo(left, top + cornerLength)
            lineTo(left, top + cornerRadiusPx)
            quadraticTo(left, top, left + cornerRadiusPx, top)
            lineTo(left + cornerLength, top)
        }
        drawPath(pathTL, scannerColor, style = Stroke(strokeWidth))

        // Top-Right
        val pathTR = Path().apply {
            moveTo(right - cornerLength, top)
            lineTo(right - cornerRadiusPx, top)
            quadraticTo(right, top, right, top + cornerRadiusPx)
            lineTo(right, top + cornerLength)
        }
        drawPath(pathTR, scannerColor, style = Stroke(strokeWidth))

        // Bottom-Right
        val pathBR = Path().apply {
            moveTo(right, bottom - cornerLength)
            lineTo(right, bottom - cornerRadiusPx)
            quadraticTo(right, bottom, right - cornerRadiusPx, bottom)
            lineTo(right - cornerLength, bottom)
        }
        drawPath(pathBR, scannerColor, style = Stroke(strokeWidth))

        // Bottom-Left
        val pathBL = Path().apply {
            moveTo(left + cornerLength, bottom)
            lineTo(left + cornerRadiusPx, bottom)
            quadraticTo(left, bottom, left, bottom - cornerRadiusPx)
            lineTo(left, bottom - cornerLength)
        }
        drawPath(pathBL, scannerColor, style = Stroke(strokeWidth))

        // Draw laser scanning line with gradient glow
        val laserY = top + (boxSide * laserYRatio)
        drawLine(
            color = scannerColor,
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}
