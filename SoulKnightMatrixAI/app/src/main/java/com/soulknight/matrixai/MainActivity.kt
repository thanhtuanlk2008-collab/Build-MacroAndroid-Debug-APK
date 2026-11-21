package com.soulknight.matrixai

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.soulknight.matrixai.capture.ScreenCaptureService
import com.soulknight.matrixai.macro.MacroConfig
import com.soulknight.matrixai.macro.MacroProfileStore
import com.soulknight.matrixai.macro.MacroScheduler
import com.soulknight.matrixai.input.MatrixAccessibilityService

class MainActivity : ComponentActivity() {
    private var accessibilityConnected by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        accessibilityConnected = MatrixAccessibilityService.isConnected()
    }
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = MacroProfileStore(this)
        val targetStore = InputTargetStore(this)
        setContent {
            var config by remember { mutableStateOf(store.load()) }
            var captureStatus by remember { mutableStateOf("Chưa cấp quyền chụp màn hình") }
            var macroStatus by remember { mutableStateOf("Macro đang dừng") }
            var lastEvent by remember { mutableStateOf("Chưa có sự kiện") }
            var profileName by remember { mutableStateOf("Cấu hình mới") }
            var profiles by remember { mutableStateOf(store.profileNames()) }
            val mainHandler = remember { Handler(Looper.getMainLooper()) }
            val scheduler = remember {
                MacroScheduler(
                    onAction = { action ->
                        // Compose state must only be changed on the main thread.
                        mainHandler.post { lastEvent = "Đến lịch: ${action.displayName}" }
                    },
                    onError = { action, error ->
                        mainHandler.post {
                            lastEvent = "Lỗi callback ${action.displayName}: ${error.message ?: "không rõ"}"
                        }
                    }
                )
            }
            DisposableEffect(Unit) {
                onDispose { scheduler.stop() }
            }

            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Soul Knight Matrix AI", style = MaterialTheme.typography.headlineMedium)
                        Text("Macro Engine · Bản thử nghiệm cấu hình", style = MaterialTheme.typography.titleMedium)

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("1. Cấu hình Macro", style = MaterialTheme.typography.titleLarge)
                                MacroSetting(
                                    title = "Tấn công",
                                    enabled = config.attackEnabled,
                                    intervalMs = config.attackIntervalMs,
                                    min = 100f,
                                    max = 5_000f,
                                    onEnabled = { config = config.copy(attackEnabled = it) },
                                    onInterval = { config = config.copy(attackIntervalMs = it.toLong()) }
                                )
                                HorizontalDivider()
                                MacroSetting(
                                    title = "Sử dụng kỹ năng",
                                    enabled = config.skillEnabled,
                                    intervalMs = config.skillIntervalMs,
                                    min = 500f,
                                    max = 60_000f,
                                    onEnabled = { config = config.copy(skillEnabled = it) },
                                    onInterval = { config = config.copy(skillIntervalMs = it.toLong()) }
                                )
                                HorizontalDivider()
                                MacroSetting(
                                    title = "Đổi vũ khí",
                                    enabled = config.switchWeaponEnabled,
                                    intervalMs = config.switchWeaponIntervalMs,
                                    min = 500f,
                                    max = 60_000f,
                                    onEnabled = { config = config.copy(switchWeaponEnabled = it) },
                                    onInterval = { config = config.copy(switchWeaponIntervalMs = it.toLong()) }
                                )
                                Button(
                                    onClick = {
                                        store.save(config)
                                        macroStatus = "Đã lưu cấu hình mặc định"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Lưu cấu hình mặc định") }

                                HorizontalDivider()
                                Text("Cấu hình đã lưu", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    label = { Text("Tên cấu hình mới") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        try {
                                            store.saveProfile(profileName, config)
                                            profiles = store.profileNames()
                                            macroStatus = "Đã lưu hồ sơ: ${profileName.trim()}"
                                        } catch (error: IllegalArgumentException) {
                                            macroStatus = error.message ?: "Tên cấu hình không hợp lệ"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Lưu thành cấu hình riêng") }
                                profiles.forEach { name ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(name, modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            config = store.loadProfile(name)
                                            profileName = name
                                            macroStatus = "Đã tải cấu hình: $name"
                                        }) { Text("Tải") }
                                        if (name != MacroProfileStore.DEFAULT_PROFILE) {
                                            TextButton(onClick = {
                                                store.deleteProfile(name)
                                                profiles = store.profileNames()
                                                macroStatus = "Đã xóa cấu hình: $name"
                                            }) { Text("Xóa") }
                                        }
                                    }
                                }
                            }
                        }

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("2. Bộ lập lịch Macro", style = MaterialTheme.typography.titleLarge)
                                Text(macroStatus)
                                Text("Sự kiện gần nhất: $lastEvent", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            store.save(config)
                                            val started = scheduler.start(config)
                                            macroStatus = if (started) {
                                                "Đang chạy bộ lập lịch (chế độ kiểm thử)"
                                            } else {
                                                "Không thể chạy: hãy bật ít nhất một chức năng"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Chạy") }
                                    OutlinedButton(
                                        onClick = {
                                            scheduler.stop()
                                            macroStatus = "Macro đang dừng"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Dừng") }
                                }
                                Button(
                                    onClick = {
                                        scheduler.stop()
                                        macroStatus = "ĐÃ DỪNG KHẨN CẤP"
                                        lastEvent = "Đã yêu cầu dừng toàn bộ tác vụ Macro"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Emergency Stop") }
                                Text(
                                    "Lưu ý: hiện tại ứng dụng chỉ phát sự kiện theo lịch để kiểm thử. " +
                                        "Chưa gửi thao tác chạm vào game.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("3. Quyền điều khiển cảm ứng", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (accessibilityConnected) "AccessibilityService đã kết nối"
                                    else "Chưa kết nối. Bạn cần tự bật dịch vụ trong Cài đặt Android.",
                                    color = if (accessibilityConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Mở cài đặt Accessibility") }
                                Text(
                                    "InputController đã được thêm làm cổng gửi thao tác. " +
                                        "Macro chưa tự gửi chạm: cần cấu hình tọa độ nút và kiểm thử trên thiết bị thật. " +
                                        "Android không bảo đảm đa chạm độc lập.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("4. Chụp màn hình", style = MaterialTheme.typography.titleLarge)
                                Text(captureStatus)
                                Button(
                                    onClick = {
                                        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        captureLauncher.launch(manager.createScreenCaptureIntent())
                                        captureStatus = "Đang chờ bạn cấp quyền..."
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Cấp quyền chụp màn hình") }
                                OutlinedButton(
                                    onClick = {
                                        stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                                        captureStatus = "Đã yêu cầu dừng dịch vụ"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Dừng chụp màn hình") }
                            }
                        }
                        Text(
                            "Macro Engine và AI Engine được tách riêng. Điều khiển cảm ứng thực tế " +
                                "cần InputController có quyền phù hợp và kiểm thử trên thiết bị.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroSetting(
    title: String,
    enabled: Boolean,
    intervalMs: Long,
    min: Float,
    max: Float,
    onEnabled: (Boolean) -> Unit,
    onInterval: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = onEnabled)
        }
        Text("Chu kỳ: ${intervalMs} ms", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = intervalMs.toFloat().coerceIn(min, max),
            onValueChange = onInterval,
            valueRange = min..max,
            enabled = enabled
        )
    }
}
