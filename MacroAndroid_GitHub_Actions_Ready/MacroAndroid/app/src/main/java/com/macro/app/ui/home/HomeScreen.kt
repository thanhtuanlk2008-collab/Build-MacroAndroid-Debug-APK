package com.macro.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.engine.PlaybackTelemetry
import com.macro.app.engine.SpeedEngine
import com.macro.app.model.Macro
import com.macro.app.model.MacroState
import com.macro.app.ui.theme.GamingColors
import com.macro.app.utils.PermissionManager
import com.macro.app.utils.PermissionStatus

@Composable
fun HomeScreen(
    macros: List<Macro>,
    permissionStatus: PermissionStatus,
    telemetry: PlaybackTelemetry,
    currentSpeed: Float,
    instantMode: Boolean,
    floatingActive: Boolean,
    onCreateMacro: () -> Unit,
    onOpenRecorder: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTesting: () -> Unit,
    onToggleFloating: () -> Unit,
    onPlayMacro: (Macro) -> Unit,
    onEditMacro: (Macro) -> Unit,
    onPauseMacro: () -> Unit,
    onResumeMacro: () -> Unit,
    onStopMacro: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onInstantModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gaming Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MACROENGINE PRO",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = GamingColors.TextPrimary
                        )
                    }
                    Text(
                        text = "Gaming Touch Automation • Non-Root (Android 10–16)",
                        style = MaterialTheme.typography.bodySmall,
                        color = GamingColors.TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (permissionStatus.accessibilityEnabled)
                                GamingColors.NeonGreen.copy(alpha = 0.15f)
                            else
                                GamingColors.AmberWarn.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (permissionStatus.accessibilityEnabled) "ENGINE READY" else "SETUP NEEDED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (permissionStatus.accessibilityEnabled) GamingColors.NeonGreen else GamingColors.AmberWarn
                    )
                }
            }
        }

        // Permission Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TRẠNG THÁI QUYỀN HỆ THỐNG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    PermissionRow(
                        title = "Accessibility Service (Tự động chạm/vuốt)",
                        granted = permissionStatus.accessibilityEnabled,
                        actionLabel = if (permissionStatus.accessibilityEnabled) "Đã bật" else "Bật ngay",
                        onClick = { PermissionManager.openAccessibilitySettings(context) }
                    )

                    PermissionRow(
                        title = "Hiển thị trên ứng dụng khác (Bảng nổi)",
                        granted = permissionStatus.overlayEnabled,
                        actionLabel = if (permissionStatus.overlayEnabled) "Đã cấp" else "Cấp quyền",
                        onClick = { PermissionManager.openOverlaySettings(context) }
                    )

                    if (!permissionStatus.accessibilityEnabled) {
                        Text(
                            text = "Mẹo Android 13–16 (vivo Y31d): Nếu nút bật Accessibility bị mờ khi cài APK ngoài, hãy mở Chi tiết ứng dụng → menu ⋮ góc trên → 'Cho phép cài đặt bị hạn chế'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamingColors.AmberWarn
                        )
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Add,
                        title = "Tạo Macro",
                        subtitle = "Thủ công & Timeline",
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onCreateMacro
                    )
                    QuickActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.RadioButtonChecked,
                        title = "Ghi Thao Tác",
                        subtitle = "Touch Pad & Pin",
                        accent = GamingColors.CrimsonRed,
                        onClick = onOpenRecorder
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Layers,
                        title = if (floatingActive) "Tắt Bảng Nổi" else "Bật Bảng Nổi",
                        subtitle = if (floatingActive) "Đang hiển thị" else "Điều khiển trên game",
                        accent = if (floatingActive) GamingColors.NeonGreen else GamingColors.CyberBlue,
                        onClick = {
                            if (!permissionStatus.overlayEnabled) {
                                PermissionManager.openOverlaySettings(context)
                            } else {
                                onToggleFloating()
                            }
                        }
                    )
                    QuickActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderOpen,
                        title = "Thư Viện (${macros.size})",
                        subtitle = "Quản lý & JSON",
                        accent = GamingColors.CyberBlue,
                        onClick = onOpenLibrary
                    )
                }

                OutlinedButton(
                    onClick = onOpenTesting,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mở Testing Dashboard & Kiểm thử Đa chạm")
                }
            }
        }

        // Live Speed Engine & Playback Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.ElevatedCard),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ADVANCED SPEED ENGINE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tốc độ trực tiếp: ${String.format("%.2fx", currentSpeed).replace(".00x", ".0x")}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamingColors.TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Instant", style = MaterialTheme.typography.labelMedium, color = GamingColors.TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(checked = instantMode, onCheckedChange = onInstantModeChange)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SpeedEngine.PRESET_SPEEDS) { spd ->
                            val selected = kotlin.math.abs(currentSpeed - spd) < 0.05f
                            FilterChip(
                                selected = selected,
                                onClick = { onSpeedChange(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }

                    // Active Playback Status
                    if (telemetry.state == MacroState.PLAYING || telemetry.state == MacroState.PAUSED) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GamingColors.ObsidianBg, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${telemetry.activeMacroName} (${telemetry.state.name})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GamingColors.NeonGreen
                                )
                                Text(
                                    text = "Bước ${telemetry.currentActionIndex}/${telemetry.totalActions} • Vòng ${telemetry.currentLoop}/${if (telemetry.targetLoops == -1) "∞" else telemetry.targetLoops}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GamingColors.TextSecondary
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        if (telemetry.state == MacroState.PLAYING) onPauseMacro() else onResumeMacro()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (telemetry.state == MacroState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Pause/Resume",
                                        tint = GamingColors.AmberWarn
                                    )
                                }
                                IconButton(onClick = onStopMacro) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Emergency Stop",
                                        tint = GamingColors.CrimsonRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Macros
        item {
            Text(
                text = "MACRO GẦN ĐÂY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = GamingColors.TextSecondary
            )
        }

        items(macros.take(5), key = { it.id }) { macro ->
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp),
                onClick = { onEditMacro(macro) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = macro.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GamingColors.TextPrimary
                        )
                        Text(
                            text = "${macro.gameCategory.displayName} • ${macro.actions.size} bước • ${macro.defaultSpeed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamingColors.TextSecondary
                        )
                    }

                    Button(
                        onClick = { onPlayMacro(macro) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chạy")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) GamingColors.NeonGreen else GamingColors.AmberWarn,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = GamingColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GamingColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
        }
    }
}
