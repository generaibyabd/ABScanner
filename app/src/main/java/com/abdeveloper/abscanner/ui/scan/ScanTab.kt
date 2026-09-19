package com.abdeveloper.abscanner.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.abdeveloper.abscanner.camera.CameraPreviewView
import com.abdeveloper.abscanner.camera.DetectedBarcode
import com.abdeveloper.abscanner.data.ScanItem
import com.abdeveloper.abscanner.ui.ScannerViewModel
import com.abdeveloper.abscanner.ui.i18n.Strings
import com.abdeveloper.abscanner.ui.theme.BrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanTab(
    viewModel: ScannerViewModel,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.decodeGalleryUri(uri)
        }
    }

    val torchEnabled by viewModel.torchEnabled.collectAsState()
    val hasFlashUnit by viewModel.hasFlashUnit.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val isBatchMode by viewModel.isBatchMode.collectAsState()
    val batchScans by viewModel.batchScans.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val isCurrentSaved by viewModel.isCurrentSaved.collectAsState()
    val galleryProcessing by viewModel.galleryProcessing.collectAsState()
    val galleryMessage by viewModel.galleryMessage.collectAsState()

    var showBatchSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreviewView(
                torchEnabled = torchEnabled,
                zoomLevel = zoomLevel,
                onBarcodesDetected = { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        viewModel.onBarcodeDetected(barcodes.first())
                    }
                },
                onBarcodeSelected = { barcode ->
                    viewModel.onBarcodeDetected(barcode)
                },
                onHasFlashUnitChanged = { viewModel.setFlashAvailable(it) }
            )
        } else {
            // Permission Rationale Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BrandBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = BrandBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = strings.cameraPermissionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = strings.cameraPermissionDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text(text = strings.grantPermission, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Floating Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight button
            if (hasFlashUnit) {
                Surface(
                    shape = CircleShape,
                    color = if (torchEnabled) BrandBlue else Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleTorch() }) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight"
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }

            // Batch mode pill toggle
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isBatchMode) BrandBlue else Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier.clickable { viewModel.toggleBatchMode() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Batch Mode",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.batchMode + if (isBatchMode && batchScans.isNotEmpty()) " (${batchScans.size})" else "",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gallery Photo Picker button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = strings.pickFromGallery
                    )
                }
            }
        }

        // Bottom Zoom Chips
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(1.0f to "1×", 2.0f to "2×", 5.0f to "5×").forEach { (zoom, label) ->
                val isSelected = kotlin.math.abs(zoomLevel - zoom) < 0.2f
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) BrandBlue else Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { viewModel.setZoom(zoom) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // If batch scans exist, show batch drawer button
            if (isBatchMode && batchScans.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandBlue,
                    contentColor = Color.White,
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { showBatchSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Batch (${batchScans.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Gallery Processing overlay
        if (galleryProcessing) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BrandBlue)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Decoding image...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Gallery Error Dialog
        galleryMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissGalleryMessage() },
                title = { Text(text = "Scan from Image") },
                text = { Text(text = msg) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissGalleryMessage() }) {
                        Text(text = "OK")
                    }
                }
            )
        }

        // Scanned Result Sheet
        currentResult?.let { parsed ->
            ScanResultDialog(
                parsed = parsed,
                isSaved = isCurrentSaved,
                strings = strings,
                onDismiss = { viewModel.dismissResult() },
                onPrimaryAction = { viewModel.executePrimaryAction(parsed) },
                onCopy = { text, isSensitive -> viewModel.copyToClipboard(text, isSensitive) },
                onShare = { text -> viewModel.shareText(text) },
                onToggleSave = { viewModel.toggleCurrentSaved() }
            )
        }

        // Batch Scans Sheet
        if (showBatchSheet) {
            val batchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBatchSheet = false },
                sheetState = batchSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Batch Scans (${batchScans.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = {
                            viewModel.clearBatch()
                            showBatchSheet = false
                        }) {
                            Text(text = "Clear")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(batchScans) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.rawValue,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    IconButton(onClick = { viewModel.copyToClipboard(item.rawValue) }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Batch Export CSV / Copy All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = "Index,Format,Value\n" + batchScans.mapIndexed { idx, item ->
                                    "$idx,${item.formatName},\"${item.rawValue.replace("\"", "\"\"")}\""
                                }.joinToString("\n")
                                viewModel.shareText(csv)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                val all = batchScans.joinToString("\n") { it.rawValue }
                                viewModel.copyToClipboard(all)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy All")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
