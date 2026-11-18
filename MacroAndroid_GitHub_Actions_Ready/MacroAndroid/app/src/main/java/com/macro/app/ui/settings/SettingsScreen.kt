package com.macro.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.BuildConfig
import com.macro.app.engine.SpeedEngine
import com.macro.app.model.AccentTheme
import com.macro.app.model.MacroSettings
import com.macro.app.ui.theme.GamingColors
import com.macro.app.utils.PermissionManager

@Composable
fun SettingsScreen(
    settings: MacroSettings,
    onUpdateSettings: ((MacroSettings) -> MacroSettings) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "CÀI ĐẶT HỆ THỐNG",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Theme & Speed Defaults
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Giao diện điểm nhấn", style = MaterialTheme.typography.labelLarge, color = GamingColors.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = settings.accentTheme == theme,
                                onClick = { onUpdateSettings { it.copy(accentTheme = theme) } },
                                label = { Text(theme.displayName) }
                            )
                        }
                    }

                    Text("Tốc độ mặc định: ${settings.defaultSpeed}x", style = MaterialTheme.typography.labelLarge, color = GamingColors.TextPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SpeedEngine.PRESET_SPEEDS) { spd ->
                            FilterChip(
                                selected = kotlin.math.abs(settings.defaultSpeed - spd) < 0.05f,
                                onClick = { onUpdateSettings { it.copy(defaultSpeed = spd) } },
                                label = { Text("${spd}x") }
                            )
                        }
                    }
                }
            }
        }

        // Floating Overlay Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bảng điều khiển nổi (Floating Control)", style = MaterialTheme.typography.labelLarge, color = GamingColors.TextPrimary)

                    Text("Độ trong suốt: ${(settings.floatingAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                    Slider(
                        value = settings.floatingAlpha,
                        onValueChange = { v -> onUpdateSettings { it.copy(floatingAlpha = v) } },
                        valueRange = 0.35f..1.0f
                    )

                    Text("Kích thước bảng nổi: ${String.format("%.2fx", settings.floatingScale)}", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                    Slider(
                        value = settings.floatingScale,
                        onValueChange = { v -> onUpdateSettings { it.copy(floatingScale = v) } },
                        valueRange = 0.75f..1.35f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tự bỏ qua xung đột chạm tay", style = MaterialTheme.typography.bodyMedium, color = GamingColors.TextPrimary)
                            Text("Không dừng Macro khi ngón tay chạm đồng thời", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.safeTouchRetryOnCancel,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(safeTouchRetryOnCancel = v) } }
                        )
                    }
                }
            }
        }

        // Permissions Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quản lý quyền & Tối ưu vivo Y31d", style = MaterialTheme.typography.labelLarge, color = GamingColors.TextPrimary)
                    OutlinedButton(
                        onClick = { PermissionManager.openAccessibilitySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cài đặt AccessibilityService")
                    }
                    OutlinedButton(
                        onClick = { PermissionManager.openOverlaySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cài đặt Hiển thị trên ứng dụng khác")
                    }
                    OutlinedButton(
                        onClick = { PermissionManager.openBatteryOptimizationSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tắt tối ưu hóa pin (giữ Bảng nổi ổn định)")
                    }
                    OutlinedButton(
                        onClick = { PermissionManager.openAppDetailsSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chi tiết ứng dụng (Cho phép cài đặt bị hạn chế)")
                    }
                }
            }
        }

        // App Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Thông tin ứng dụng", style = MaterialTheme.typography.labelLarge, color = GamingColors.TextPrimary)
                    Text("MacroEngine Pro v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                    Text("Hoạt động hoàn toàn ngoại tuyến, không thu thập dữ liệu, không yêu cầu Root.", style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
                }
            }
        }
    }
}
