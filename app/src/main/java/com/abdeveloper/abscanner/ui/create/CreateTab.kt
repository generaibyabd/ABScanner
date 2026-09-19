package com.abdeveloper.abscanner.ui.create

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdeveloper.abscanner.codec.CheckDigitValidator
import com.abdeveloper.abscanner.codec.CodeType
import com.abdeveloper.abscanner.generator.ModuleShape
import com.abdeveloper.abscanner.ui.ScannerViewModel
import com.abdeveloper.abscanner.ui.i18n.Strings
import com.abdeveloper.abscanner.ui.theme.BrandBlue
import com.abdeveloper.abscanner.ui.theme.DangerRed
import com.abdeveloper.abscanner.ui.theme.SafetyGreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTab(
    viewModel: ScannerViewModel,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(CodeType.TEXT) }
    var selectedFormat by remember { mutableStateOf(BarcodeFormat.QR_CODE) }

    // Dynamic input fields
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var emailTo by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var smsPhone by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }
    var barcodeDigits by remember { mutableStateOf("") }

    // Styling
    var selectedFgColor by remember { mutableStateOf(Color.Black) }
    var selectedBgColor by remember { mutableStateOf(Color.White) }
    var selectedShape by remember { mutableStateOf(ModuleShape.SQUARE) }
    var selectedEcc by remember { mutableStateOf(ErrorCorrectionLevel.M) }

    val generatedBitmap by viewModel.generatedBitmap.collectAsState()
    val verification by viewModel.generationVerification.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val typeOptions = listOf(
        CodeType.TEXT to ("Text" to Icons.Default.TextFields),
        CodeType.URL to ("Website" to Icons.Default.Link),
        CodeType.WIFI to ("Wi-Fi" to Icons.Default.Wifi),
        CodeType.CONTACT to ("Contact" to Icons.Default.ContactPhone),
        CodeType.EMAIL to ("Email" to Icons.Default.Email),
        CodeType.PHONE to ("Phone" to Icons.Default.Phone),
        CodeType.SMS to ("SMS" to Icons.Default.Sms),
        CodeType.PRODUCT to ("Barcode" to Icons.Default.QrCode)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = strings.tabCreate,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content Type Selection Chips
        Text(
            text = "Select Content Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            typeOptions.forEach { (type, pair) ->
                val (label, icon) = pair
                val isSelected = selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedType = type
                        selectedFormat = if (type == CodeType.PRODUCT) BarcodeFormat.EAN_13 else BarcodeFormat.QR_CODE
                    },
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Fields Form
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedType) {
                    CodeType.TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Text or Note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                    CodeType.URL -> {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Website URL") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                    }
                    CodeType.WIFI -> {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Network Name (SSID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WPA", "WEP", "NOPASS").forEach { sec ->
                                FilterChip(
                                    selected = wifiSecurity == sec,
                                    onClick = { wifiSecurity = sec },
                                    label = { Text(if (sec == "NOPASS") "Open" else sec) }
                                )
                            }
                        }
                    }
                    CodeType.CONTACT -> {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                    CodeType.EMAIL -> {
                        OutlinedTextField(
                            value = emailTo,
                            onValueChange = { emailTo = it },
                            label = { Text("Recipient Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailSubject,
                            onValueChange = { emailSubject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailBody,
                            onValueChange = { emailBody = it },
                            label = { Text("Body") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    CodeType.PHONE -> {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                    CodeType.SMS -> {
                        OutlinedTextField(
                            value = smsPhone,
                            onValueChange = { smsPhone = it },
                            label = { Text("Recipient Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = smsMessage,
                            onValueChange = { smsMessage = it },
                            label = { Text("Message Body") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    CodeType.PRODUCT -> {
                        OutlinedTextField(
                            value = barcodeDigits,
                            onValueChange = {
                                val clean = it.filter { c -> c.isDigit() }
                                barcodeDigits = clean
                            },
                            label = { Text("EAN-13 (12 or 13 digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Check digit calculator assist
                        if (barcodeDigits.length == 12) {
                            val check = CheckDigitValidator.computeMod10CheckDigit(barcodeDigits)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Auto Check Digit: $check (Full: $barcodeDigits$check)",
                                color = BrandBlue,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QR Customization Section (only if format is QR)
        if (selectedFormat == BarcodeFormat.QR_CODE) {
            Text(
                text = "Styling & Colors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Color Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Color.Black to "Black",
                    BrandBlue to "Brand Blue",
                    Color(0xFF1E8E3E) to "Forest",
                    Color(0xFF8E24AA) to "Purple"
                ).forEach { (col, _) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (selectedFgColor == col) 3.dp else 1.dp,
                                color = if (selectedFgColor == col) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { selectedFgColor = col }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Module Shapes
            Text(
                text = "Module Shape",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ModuleShape.SQUARE to "Square",
                    ModuleShape.ROUNDED to "Rounded",
                    ModuleShape.DOTS to "Dots"
                ).forEach { (shape, name) ->
                    FilterChip(
                        selected = selectedShape == shape,
                        onClick = { selectedShape = shape },
                        label = { Text(name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Generate Action Button
        Button(
            onClick = {
                val payload = buildPayload(
                    selectedType,
                    textInput,
                    urlInput,
                    wifiSsid,
                    wifiPassword,
                    wifiSecurity,
                    contactName,
                    contactPhone,
                    contactEmail,
                    emailTo,
                    emailSubject,
                    emailBody,
                    phoneInput,
                    smsPhone,
                    smsMessage,
                    barcodeDigits
                )

                if (payload.isNotEmpty()) {
                    viewModel.generateCode(
                        content = payload,
                        format = selectedFormat,
                        foregroundColor = selectedFgColor.toArgb(),
                        backgroundColor = selectedBgColor.toArgb(),
                        shape = selectedShape,
                        ecc = selectedEcc
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = strings.generateCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Generated Code Sheet Dialog
    generatedBitmap?.let { bmp ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val payload = buildPayload(
            selectedType,
            textInput,
            urlInput,
            wifiSsid,
            wifiPassword,
            wifiSecurity,
            contactName,
            contactPhone,
            contactEmail,
            emailTo,
            emailSubject,
            emailBody,
            phoneInput,
            smsPhone,
            smsMessage,
            barcodeDigits
        )

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissGenerated() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Generated Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.dismissGenerated() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barcode / QR Preview Image
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp,
                    color = Color.White,
                    modifier = Modifier.size(260.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Generated Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Badge
                verification?.let { ver ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (ver.isScannable) SafetyGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (ver.isScannable) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (ver.isScannable) SafetyGreen else DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (ver.isScannable) strings.scannabilityVerified else (ver.errorMessage ?: "Unreadable"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (ver.isScannable) SafetyGreen else DangerRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Save PNG, Share, Save to DB
                Button(
                    onClick = { viewModel.saveBitmapToGallery(bmp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Download, contentDescription = strings.savePng)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.savePng, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shareBitmap(bmp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = strings.share, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.share)
                    }

                    OutlinedButton(
                        onClick = { viewModel.saveGeneratedToHistory(payload, selectedFormat) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = strings.save, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.save)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

private fun buildPayload(
    type: CodeType,
    text: String,
    url: String,
    ssid: String,
    pass: String,
    security: String,
    name: String,
    phone: String,
    email: String,
    emailTo: String,
    subject: String,
    body: String,
    tel: String,
    smsPhone: String,
    smsBody: String,
    barcode: String
): String {
    return when (type) {
        CodeType.TEXT -> text.ifEmpty { "ABScanner text" }
        CodeType.URL -> url
        CodeType.WIFI -> "WIFI:S:$ssid;T:$security;P:$pass;;"
        CodeType.CONTACT -> "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEMAIL:$email\nEND:VCARD"
        CodeType.EMAIL -> "mailto:$emailTo?subject=$subject&body=$body"
        CodeType.PHONE -> "tel:$tel"
        CodeType.SMS -> "smsto:$smsPhone:$smsBody"
        CodeType.PRODUCT -> {
            if (barcode.length == 12) {
                val check = CheckDigitValidator.computeMod10CheckDigit(barcode)
                "$barcode$check"
            } else barcode.ifEmpty { "1234567890128" }
        }
        else -> text
    }
}
