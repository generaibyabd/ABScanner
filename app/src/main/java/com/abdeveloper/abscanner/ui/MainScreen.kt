package com.abdeveloper.abscanner.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abdeveloper.abscanner.ui.create.CreateTab
import com.abdeveloper.abscanner.ui.i18n.AppLocales
import com.abdeveloper.abscanner.ui.i18n.Strings
import com.abdeveloper.abscanner.ui.scan.ScanTab
import com.abdeveloper.abscanner.ui.settings.SettingsTab
import com.abdeveloper.abscanner.ui.theme.BrandBlue

@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val settings by viewModel.userSettings.collectAsState()
    val strings = remember(settings.languageCode) { Strings(settings.languageCode) }
    val isRtl = remember(settings.languageCode) { AppLocales.getLanguage(settings.languageCode).isRtl }
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    var selectedTab by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpanded = maxWidth >= 600.dp

            if (isExpanded) {
                // Adaptive Tablet / Desktop NavigationRail
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                                    contentDescription = strings.tabScan
                                )
                            },
                            label = { Text(strings.tabScan) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                indicatorColor = BrandBlue.copy(alpha = 0.15f)
                            )
                        )

                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                    contentDescription = strings.tabCreate
                                )
                            },
                            label = { Text(strings.tabCreate) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                indicatorColor = BrandBlue.copy(alpha = 0.15f)
                            )
                        )

                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = strings.tabSettings
                                )
                            },
                            label = { Text(strings.tabSettings) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                indicatorColor = BrandBlue.copy(alpha = 0.15f)
                            )
                        )
                    }

                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.weight(1f)
                    ) { padding ->
                        when (selectedTab) {
                            0 -> ScanTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                            1 -> CreateTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                            2 -> SettingsTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                        }
                    }
                }
            } else {
                // Mobile Portrait NavigationBar
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                                        contentDescription = strings.tabScan
                                    )
                                },
                                label = { Text(strings.tabScan) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandBlue,
                                    indicatorColor = BrandBlue.copy(alpha = 0.15f)
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                        contentDescription = strings.tabCreate
                                    )
                                },
                                label = { Text(strings.tabCreate) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandBlue,
                                    indicatorColor = BrandBlue.copy(alpha = 0.15f)
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = strings.tabSettings
                                    )
                                },
                                label = { Text(strings.tabSettings) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandBlue,
                                    indicatorColor = BrandBlue.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        0 -> ScanTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                        1 -> CreateTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                        2 -> SettingsTab(viewModel = viewModel, strings = strings, modifier = Modifier.padding(padding))
                    }
                }
            }
        }
    }
}
