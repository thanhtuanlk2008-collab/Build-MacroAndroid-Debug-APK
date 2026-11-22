package com.macro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macro.app.model.Macro
import com.macro.app.ui.MacroViewModel
import com.macro.app.ui.editor.MacroEditorScreen
import com.macro.app.ui.home.HomeScreen
import com.macro.app.ui.library.MacroLibraryScreen
import com.macro.app.ui.settings.SettingsScreen
import com.macro.app.ui.testing.TestingDashboardScreen
import com.macro.app.ui.biome.BiomeReviewScreen
import com.macro.app.ui.theme.GamingColors
import com.macro.app.ui.theme.MacroTheme
import com.macro.app.utils.PermissionManager

enum class NavTab(val label: String) {
    HOME("Trang chủ"),
    EDITOR("Trình sửa"),
    LIBRARY("Thư viện"),
    TESTING("Kiểm thử"),
    BIOME("Biome"),
    SETTINGS("Cài đặt")
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled via resume refresh */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionManager.isNotificationPermissionGranted(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MacroAppRoot()
        }
    }
}

@Composable
fun MacroAppRoot(viewModel: MacroViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val macros by viewModel.macros.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val speed by viewModel.speedMultiplier.collectAsState()
    val instantMode by viewModel.instantMode.collectAsState()
    val floatingActive by viewModel.floatingActive.collectAsState()
    val editingMacro by viewModel.editingMacro.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val pendingBiomeFeedback by viewModel.pendingBiomeFeedback.collectAsState()

    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MacroTheme(accentTheme = settings.accentTheme) {
        Scaffold(
            containerColor = GamingColors.ObsidianBg,
            bottomBar = {
                NavigationBar(containerColor = GamingColors.CarbonSurface) {
                    NavTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            NavTab.HOME -> Icons.Default.Home
                            NavTab.EDITOR -> Icons.Default.Edit
                            NavTab.LIBRARY -> Icons.Default.FolderOpen
                            NavTab.TESTING -> Icons.Default.Science
                            NavTab.BIOME -> Icons.Default.Palette
                            NavTab.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentTab) {
                    NavTab.HOME -> HomeScreen(
                        macros = macros,
                        permissionStatus = permissionStatus,
                        telemetry = telemetry,
                        currentSpeed = speed,
                        instantMode = instantMode,
                        floatingActive = floatingActive,
                        onCreateMacro = {
                            viewModel.startEditingNewMacro()
                            currentTab = NavTab.EDITOR
                        },
                        onOpenRecorder = {
                            if (!floatingActive) {
                                viewModel.toggleFloatingControl()
                            }
                            viewModel.startEditingNewMacro()
                            currentTab = NavTab.EDITOR
                        },
                        onOpenLibrary = { currentTab = NavTab.LIBRARY },
                        onOpenTesting = { currentTab = NavTab.TESTING },
                        onToggleFloating = { viewModel.toggleFloatingControl() },
                        onPlayMacro = { viewModel.playMacro(it) },
                        onEditMacro = {
                            viewModel.startEditingMacro(it)
                            currentTab = NavTab.EDITOR
                        },
                        onPauseMacro = { viewModel.pauseMacro() },
                        onResumeMacro = { viewModel.resumeMacro() },
                        onStopMacro = { viewModel.stopMacro() },
                        onSpeedChange = { viewModel.setSpeed(it) },
                        onInstantModeChange = { viewModel.setInstantMode(it) }
                    )

                    NavTab.EDITOR -> {
                        val activeMacro = editingMacro ?: macros.firstOrNull() ?: Macro(name = "Macro Mới")
                        MacroEditorScreen(
                            macro = activeMacro,
                            onMacroChange = { viewModel.updateEditingMacro(it) },
                            onSave = { viewModel.saveMacro(it) },
                            onPlayTest = { viewModel.playMacro(it) }
                        )
                    }

                    NavTab.LIBRARY -> MacroLibraryScreen(
                        macros = macros,
                        onCreateMacro = {
                            viewModel.startEditingNewMacro()
                            currentTab = NavTab.EDITOR
                        },
                        onPlayMacro = { viewModel.playMacro(it) },
                        onEditMacro = {
                            viewModel.startEditingMacro(it)
                            currentTab = NavTab.EDITOR
                        },
                        onDuplicateMacro = { viewModel.duplicateMacro(it) },
                        onDeleteMacro = { viewModel.deleteMacro(it) },
                        onExportMacroJson = { viewModel.exportMacroJson(it) },
                        onExportAllJson = { viewModel.exportAllJson() },
                        onImportJson = { raw, cb -> viewModel.importJson(raw, cb) }
                    )

                    NavTab.BIOME -> BiomeReviewScreen(
                        pending = pendingBiomeFeedback,
                        onConfirm = { id, biome -> viewModel.confirmBiomeFeedback(id, biome) },
                        onDiscard = { viewModel.discardBiomeFeedback(it) },
                        onRefresh = { viewModel.refreshBiomeFeedback() }
                    )

                    NavTab.TESTING -> TestingDashboardScreen(
                        isAccessibilityConnected = isAccessibilityConnected,
                        floatingActive = floatingActive,
                        telemetry = telemetry,
                        currentSpeed = speed,
                        onRunTouchConflictTestMacro = { viewModel.playMacro(it) },
                        onStopMacro = { viewModel.stopMacro() }
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) }
                    )
                }
            }
        }
    }
}
