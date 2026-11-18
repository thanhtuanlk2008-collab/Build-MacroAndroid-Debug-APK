package com.macro.app.ui.testing

import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macro.app.engine.PlaybackTelemetry
import com.macro.app.model.ActionType
import com.macro.app.model.GameCategory
import com.macro.app.model.Macro
import com.macro.app.model.MacroAction
import com.macro.app.model.RepeatMode
import com.macro.app.ui.theme.GamingColors

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TestingDashboardScreen(
    isAccessibilityConnected: Boolean,
    shizukuProbeStatus: String,
    onProbeShizuku: () -> Unit,
    floatingActive: Boolean,
    telemetry: PlaybackTelemetry,
    currentSpeed: Float,
    onRunTouchConflictTestMacro: (Macro) -> Unit,
    onStopMacro: () -> Unit
) {
    var activePointers by remember { mutableIntStateOf(0) }
    var totalHumanEvents by remember { mutableIntStateOf(0) }
    var lastTouchLog by remember { mutableStateOf("Chưa có thao tác thủ công trên vùng kiểm thử.") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "TESTING & DIAGNOSTICS DASHBOARD",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Giám sát dịch vụ, tốc độ thực tế và kiểm thử Human Touch Independence trên vivo Y31d",
                style = MaterialTheme.typography.bodySmall,
                color = GamingColors.TextSecondary
            )
        }

        // Live Telemetry Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.BorderSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryRow("Accessibility Service", if (isAccessibilityConnected) "ĐANG KẾT NỐI" else "CHƯA BẬT")
                    TelemetryRow("Floating Control Service", if (floatingActive) "ĐANG CHẠY" else "ĐÃ TẮT")
                    TelemetryRow("Trạng thái Macro", telemetry.state.name)
                    TelemetryRow("Tốc độ hiện tại", "${currentSpeed}x")
                    TelemetryRow("Hành động hiện tại", "${telemetry.currentActionIndex} / ${telemetry.totalActions}")
                    TelemetryRow("Vòng lặp", "${telemetry.currentLoop} / ${if (telemetry.targetLoops == -1) "∞" else telemetry.targetLoops}")
                    TelemetryRow("Thời gian chạy", "${telemetry.elapsedMs} ms")
                    TelemetryRow("Gesture thành công", "${telemetry.executedGestures}")
                    TelemetryRow("Gesture bị hủy (xung đột chạm)", "${telemetry.cancelledGestures}")
                    if (!telemetry.lastError.isNullOrBlank()) {
                        Text(
                            text = "Lỗi gần nhất: ${telemetry.lastError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamingColors.CrimsonRed
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.CarbonSurface),
                border = BorderStroke(1.dp, GamingColors.CyberBlue.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SHIZUKU INPUT BACKEND — KIỂM TRA QUYỀN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GamingColors.CyberBlue
                    )
                    Text(
                        text = "Kiểm tra UID và quyền INJECT_EVENTS ngay trong tiến trình UserService. Bước này chỉ chẩn đoán, không gửi thao tác chạm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GamingColors.TextSecondary
                    )
                    OutlinedButton(onClick = onProbeShizuku) {
                        Text("Kiểm tra Shizuku")
                    }
                    Text(
                        text = shizukuProbeStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = GamingColors.TextPrimary
                    )
                }
            }
        }

        // Human Touch Independence Interactive Test Pad
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GamingColors.ElevatedCard),
                border = BorderStroke(1.dp, GamingColors.CyberBlue.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "KIỂM THỬ THAO TÁC THỦ CÔNG SONG SONG",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GamingColors.CyberBlue
                    )
                    Text(
                        text = "Bấm 'Chạy Macro Test' (sẽ tự động chạm ở góc dưới), đồng thời bạn hãy vuốt hoặc chạm liên tục trong khung bên dưới để đo xung đột trên Android 16.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GamingColors.TextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val testMacro = Macro(
                                    name = "Touch Conflict Test",
                                    gameCategory = GameCategory.OTHER,
                                    defaultSpeed = 1.0f,
                                    repeatMode = RepeatMode.TEN_TIMES,
                                    actions = listOf(
                                        MacroAction(
                                            type = ActionType.TAP,
                                            x = 0.85f,
                                            y = 0.88f,
                                            durationMs = 40L,
                                            waitAfterMs = 600L,
                                            orderIndex = 0
                                        )
                                    )
                                )
                                onRunTouchConflictTestMacro(testMacro)
                            }
                        ) {
                            Text("Chạy Macro Test")
                        }
                        OutlinedButton(
                            onClick = onStopMacro,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GamingColors.CrimsonRed)
                        ) {
                            Text("Dừng ngay")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(GamingColors.ObsidianBg, RoundedCornerShape(10.dp))
                            .border(1.dp, GamingColors.BorderSubtle, RoundedCornerShape(10.dp))
                            .pointerInteropFilter { event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                        activePointers = event.pointerCount
                                        totalHumanEvents++
                                        lastTouchLog = "DOWN: ${event.pointerCount} ngón tại (${event.x.toInt()}, ${event.y.toInt()})"
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        activePointers = event.pointerCount
                                        totalHumanEvents++
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                                        activePointers = (event.pointerCount - 1).coerceAtLeast(0)
                                        lastTouchLog = "RELEASE: còn $activePointers ngón"
                                    }
                                }
                                true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VÙNG VUỐT & CHẠM THỦ CÔNG",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GamingColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ngón đang chạm: $activePointers • Tổng sự kiện tay: $totalHumanEvents",
                                style = MaterialTheme.typography.bodySmall,
                                color = GamingColors.NeonGreen
                            )
                            Text(
                                text = lastTouchLog,
                                style = MaterialTheme.typography.bodySmall,
                                color = GamingColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = GamingColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GamingColors.TextPrimary)
    }
}
