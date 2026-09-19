package com.abdeveloper.abscanner.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdeveloper.abscanner.data.ScanItem
import com.abdeveloper.abscanner.ui.ScannerViewModel
import com.abdeveloper.abscanner.ui.i18n.AppLocales
import com.abdeveloper.abscanner.ui.i18n.Strings
import com.abdeveloper.abscanner.ui.theme.BrandBlue
import com.abdeveloper.abscanner.ui.theme.DangerRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SubScreen {
    NONE,
    HISTORY,
    SAVED,
    LANGUAGE_PICKER,
    PRIVACY_POLICY,
    TERMS_OF_USE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: ScannerViewModel,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val savedList by viewModel.savedList.collectAsState()

    var activeSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val text = stream?.bufferedReader()?.readText() ?: ""
                    stream?.close()
                    val count = viewModel.importBackupJson(text)
                    Toast.makeText(context, "Imported $count items successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to import: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (activeSubScreen == SubScreen.HISTORY) {
        HistoryScreen(
            title = strings.history,
            items = historyList,
            strings = strings,
            onBack = { activeSubScreen = SubScreen.NONE },
            onItemClick = { viewModel.showResultFor(it) },
            onDeleteItem = { viewModel.deleteItem(it) },
            onToggleSaved = { viewModel.toggleSavedItem(it) },
            onClearHistory = { showClearHistoryDialog = true }
        )
    } else if (activeSubScreen == SubScreen.SAVED) {
        HistoryScreen(
            title = strings.savedCodes,
            items = savedList,
            strings = strings,
            onBack = { activeSubScreen = SubScreen.NONE },
            onItemClick = { viewModel.showResultFor(it) },
            onDeleteItem = { viewModel.deleteItem(it) },
            onToggleSaved = { viewModel.toggleSavedItem(it) },
            onClearHistory = null
        )
    } else {
        // Main Settings List
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = strings.tabSettings,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Appearance & Theme
            SettingsSectionHeader(title = strings.appearance)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Theme Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SYSTEM" to "Auto", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (settings.themeMode == mode) BrandBlue else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.updateTheme(mode) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.themeMode == mode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Language picker row
                    SettingsNavigationRow(
                        icon = Icons.Default.Language,
                        title = strings.language,
                        subtitle = AppLocales.getLanguage(settings.languageCode).nativeName,
                        onClick = { activeSubScreen = SubScreen.LANGUAGE_PICKER }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Data & Records
            SettingsSectionHeader(title = "Data & History")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsNavigationRow(
                        icon = Icons.Default.History,
                        title = strings.history,
                        subtitle = "${historyList.size} scanned items",
                        onClick = { activeSubScreen = SubScreen.HISTORY }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsNavigationRow(
                        icon = Icons.Default.Bookmark,
                        title = strings.savedCodes,
                        subtitle = "${savedList.size} bookmarked items",
                        onClick = { activeSubScreen = SubScreen.SAVED }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsNavigationRow(
                        icon = Icons.Default.FileUpload,
                        title = "Export JSON Backup",
                        subtitle = "Save all scan history locally",
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.exportBackupJson()
                                viewModel.shareText(json)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsNavigationRow(
                        icon = Icons.Default.FileDownload,
                        title = "Import JSON Backup",
                        subtitle = "Restore scan history from backup file",
                        onClick = { importFileLauncher.launch("application/json") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scanner Behavior
            SettingsSectionHeader(title = "Scanning Preferences")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Beep on scan",
                        checked = settings.beep,
                        onCheckedChange = { viewModel.updateBeep(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        title = "Vibrate on scan",
                        checked = settings.vibrate,
                        onCheckedChange = { viewModel.updateVibrate(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsToggleRow(
                        icon = Icons.Default.Tune,
                        title = "Keep screen awake during scan",
                        checked = settings.keepScreenOn,
                        onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsToggleRow(
                        icon = Icons.Default.Security,
                        title = "Prevent screenshots (FLAG_SECURE)",
                        checked = settings.flagSecure,
                        onCheckedChange = { viewModel.updateFlagSecure(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Legal
            SettingsSectionHeader(title = "Privacy & Legal")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsNavigationRow(
                        icon = Icons.Default.Policy,
                        title = strings.privacyPolicy,
                        subtitle = "Zero tracking • 100% offline",
                        onClick = { activeSubScreen = SubScreen.PRIVACY_POLICY }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsNavigationRow(
                        icon = Icons.Default.Info,
                        title = strings.termsOfUse,
                        subtitle = "License and user terms",
                        onClick = { activeSubScreen = SubScreen.TERMS_OF_USE }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsNavigationRow(
                        icon = Icons.Default.Delete,
                        title = "Clear All Data",
                        subtitle = "Erase all history and saved codes",
                        iconTint = DangerRed,
                        onClick = { showClearAllDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ABScanner v1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Crafted by AB Developer • Fully Offline & Private",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Language Picker Sheet
    if (activeSubScreen == SubScreen.LANGUAGE_PICKER) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSubScreen = SubScreen.NONE },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = strings.language,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(AppLocales.LANGUAGES) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(lang.code)
                                    activeSubScreen = SubScreen.NONE
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (settings.languageCode == lang.code) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Privacy Policy Dialog
    if (activeSubScreen == SubScreen.PRIVACY_POLICY) {
        AlertDialog(
            onDismissRequest = { activeSubScreen = SubScreen.NONE },
            title = { Text(text = strings.privacyPolicy) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "ABScanner is built with absolute privacy in mind:\n\n" +
                                "1. Complete Offline Operation: ABScanner never connects to external servers, analytics providers, or telemetry endpoints.\n\n" +
                                "2. Camera Stream Processing: The camera stream is analyzed entirely in real-time memory on your device using bundled ML Kit and ZXing models. No image frames are saved or transmitted.\n\n" +
                                "3. Local Data Storage: All scan history and custom generated codes are saved exclusively in an encrypted Room SQLite database on your device. You retain full control to export or erase all data at any time.\n\n" +
                                "4. Zero Third-Party Trackers: No advertising SDKs, no Google Analytics, no Firebase Crashlytics.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubScreen = SubScreen.NONE }) {
                    Text("Close")
                }
            }
        )
    }

    // Terms of Use Dialog
    if (activeSubScreen == SubScreen.TERMS_OF_USE) {
        AlertDialog(
            onDismissRequest = { activeSubScreen = SubScreen.NONE },
            title = { Text(text = strings.termsOfUse) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "ABScanner is open, reliable, and straightforward:\n\n" +
                                "• Provided 'as is' for personal and professional barcode scanning, creation, and data management.\n\n" +
                                "• ABScanner verifies URL risks using offline heuristics, but users should exercise discretion before accessing unfamiliar websites or executing financial transactions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubScreen = SubScreen.NONE }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Scan History?") },
            text = { Text("This will permanently remove all non-saved scans from your history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Erase All Data?") },
            text = { Text("This will permanently delete all scan history and bookmarked saved codes.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearAllDialog = false
                }) {
                    Text("Erase Everything", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryScreen(
    title: String,
    items: List<ScanItem>,
    strings: Strings,
    onBack: () -> Unit,
    onItemClick: (ScanItem) -> Unit,
    onDeleteItem: (ScanItem) -> Unit,
    onToggleSaved: (ScanItem) -> Unit,
    onClearHistory: (() -> Unit)?
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = items.filter {
        searchQuery.isEmpty() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.rawValue.contains(searchQuery, ignoreCase = true) ||
                it.formatName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onClearHistory != null && items.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = DangerRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search records...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No matches found." else "No records yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.formatName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.rawValue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Row {
                                IconButton(onClick = { onToggleSaved(item) }) {
                                    Icon(
                                        imageVector = if (item.isSaved) Icons.Default.Bookmark else Icons.Default.Bookmark,
                                        contentDescription = "Save",
                                        tint = if (item.isSaved) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = BrandBlue,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = BrandBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = title, tint = BrandBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue)
        )
    }
}
