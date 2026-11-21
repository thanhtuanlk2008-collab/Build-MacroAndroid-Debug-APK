package com.soulknight.matrixai

import android.app.Activity
import android.graphics.Bitmap
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.soulknight.matrixai.capture.ScreenCaptureService
import com.soulknight.matrixai.capture.FrameSnapshotStore
import com.soulknight.matrixai.macro.MacroConfig
import com.soulknight.matrixai.macro.MacroProfileStore
import com.soulknight.matrixai.macro.MacroScheduler
import com.soulknight.matrixai.input.MatrixAccessibilityService
import com.soulknight.matrixai.input.InputActionQueue
import com.soulknight.matrixai.input.InputTarget
import com.soulknight.matrixai.input.InputTargetConfig
import com.soulknight.matrixai.input.InputTargetStore
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import com.soulknight.matrixai.vision.FrameSnapshot
import com.soulknight.matrixai.movement.MovementCalibrationRunner
import com.soulknight.matrixai.movement.MovementProfile
import com.soulknight.matrixai.movement.MovementProfileStore
import com.soulknight.matrixai.movement.MovementAdaptiveSession

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
        val movementProfileStore = MovementProfileStore(this)
        setContent {
            var targets by remember { mutableStateOf(targetStore.load()) }
            val targetRef = remember { AtomicReference(targets) }
            var attackX by remember { mutableStateOf(targets.attack?.x?.toString() ?: "") }
            var attackY by remember { mutableStateOf(targets.attack?.y?.toString() ?: "") }
            var skillX by remember { mutableStateOf(targets.skill?.x?.toString() ?: "") }
            var skillY by remember { mutableStateOf(targets.skill?.y?.toString() ?: "") }
            var weaponX by remember { mutableStateOf(targets.switchWeapon?.x?.toString() ?: "") }
            var weaponY by remember { mutableStateOf(targets.switchWeapon?.y?.toString() ?: "") }
            var config by remember { mutableStateOf(store.load()) }
            var captureStatus by remember { mutableStateOf("Chưa cấp quyền chụp màn hình") }
            var frameStatus by remember { mutableStateOf("Chưa nhận được khung hình") }
            var previewFrame by remember { mutableStateOf<FrameSnapshot?>(null) }
            var playerCenterX by remember { mutableStateOf("") }
            var playerCenterY by remember { mutableStateOf("") }
            var joystickCenterX by remember { mutableStateOf("") }
            var joystickCenterY by remember { mutableStateOf("") }
            var safeCalibrationArea by remember { mutableStateOf(false) }
            var movementStatus by remember { mutableStateOf("Chưa hiệu chỉnh tốc độ") }
            var movementProfile by remember { mutableStateOf(movementProfileStore.load()) }
            var adaptiveSession by remember { mutableStateOf<MovementAdaptiveSession?>(null) }
            var adaptiveStatus by remember { mutableStateOf("Chưa bật theo dõi buff") }
            var macroStatus by remember { mutableStateOf("Macro đang dừng") }
            var lastEvent by remember { mutableStateOf("Chưa có sự kiện") }
            var profileName by remember { mutableStateOf("Cấu hình mới") }
            var profiles by remember { mutableStateOf(store.profileNames()) }
            val mainHandler = remember { Handler(Looper.getMainLooper()) }
            val calibrationRunner = remember {
                MovementCalibrationRunner(
                    inputController = MatrixAccessibilityService.inputController(),
                    handler = mainHandler,
                    screenWidth = resources.displayMetrics.widthPixels,
                    screenHeight = resources.displayMetrics.heightPixels
                ) { status, profile ->
                    mainHandler.post {
                        movementStatus = status
                        if (profile != null) {
                            movementProfile = profile
                            movementProfileStore.save(profile)
                        }
                    }
                }
            }
            val inputQueue = remember {
                InputActionQueue(
                    controller = MatrixAccessibilityService.inputController(),
                    onResult = { action, result ->
                        mainHandler.post { lastEvent = "${action.displayName}: ${result}" }
                    }
                )
            }
            val scheduler = remember {
                MacroScheduler(
                    onAction = { action ->
                        val target = targetRef.get().forAction(action)
                        if (target == null) {
                            mainHandler.post { lastEvent = "${action.displayName}: chưa cấu hình tọa độ" }
                        } else if (!inputQueue.enqueue(action, target)) {
                            mainHandler.post { lastEvent = "${action.displayName}: hàng đợi từ chối thao tác" }
                        } else {
                            mainHandler.post { lastEvent = "Đã xếp hàng: ${action.displayName}" }
                        }
                    },
                    onError = { action, error ->
                        mainHandler.post {
                            lastEvent = "Lỗi ${action.displayName}: ${error.message ?: "không rõ"}"
                        }
                    }
                )
            }
            DisposableEffect(Unit) {
                onDispose { scheduler.stop(); inputQueue.stop() }
            }
            DisposableEffect(Unit) {
                onDispose { calibrationRunner.stop(); adaptiveSession?.stop() }
            }
            DisposableEffect(Unit) {
                val frameListener: (com.soulknight.matrixai.vision.FrameSnapshot) -> Unit = { frame ->
                    mainHandler.post {
                        previewFrame = frame
                        frameStatus = "Đang nhận khung hình: ${frame.width} × ${frame.height} (xấp xỉ 10 FPS)"
                    }
                }
                FrameSnapshotStore.addListener(frameListener)
                onDispose { FrameSnapshotStore.removeListener(frameListener) }
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
                                            inputQueue.start()
                                            val started = scheduler.start(config)
                                            if (!started) inputQueue.stop()
                                            macroStatus = if (started) {
                                                "Macro đang chạy qua hàng đợi InputController"
                                            } else {
                                                "Không thể chạy: hãy bật ít nhất một chức năng"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Chạy") }
                                    OutlinedButton(
                                        onClick = {
                                            scheduler.stop()
                                            inputQueue.stop()
                                            macroStatus = "Macro đang dừng"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Dừng") }
                                }
                                Button(
                                    onClick = {
                                        scheduler.stop()
                                        inputQueue.stop()
                                        macroStatus = "ĐÃ DỪNG KHẨN CẤP"
                                        lastEvent = "Đã yêu cầu dừng toàn bộ tác vụ Macro"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Emergency Stop") }
                                Text(
                                    "Macro gửi thao tác qua InputController khi Accessibility đã bật và tọa độ hợp lệ.",
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
                                Text("Nhập tọa độ pixel của các nút trong game. Chỉ gửi thao tác sau khi Accessibility được bật.")
                                CoordinateFields("Tấn công", attackX, attackY, { attackX = it }, { attackY = it })
                                CoordinateFields("Kỹ năng", skillX, skillY, { skillX = it }, { skillY = it })
                                CoordinateFields("Đổi vũ khí", weaponX, weaponY, { weaponX = it }, { weaponY = it })
                                Button(onClick = {
                                    fun parseTarget(x: String, y: String): InputTarget? {
                                        val px = x.toFloatOrNull() ?: return null
                                        val py = y.toFloatOrNull() ?: return null
                                        return InputTarget(px, py).takeIf(InputTarget::isValid)
                                    }
                                    val updated = InputTargetConfig(
                                        attack = parseTarget(attackX, attackY),
                                        skill = parseTarget(skillX, skillY),
                                        switchWeapon = parseTarget(weaponX, weaponY)
                                    )
                                    targets = updated
                                    targetRef.set(updated)
                                    targetStore.save(updated)
                                    lastEvent = "Đã lưu tọa độ điều khiển"
                                }, modifier = Modifier.fillMaxWidth()) { Text("Lưu tọa độ") }
                            }
                        }

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("4. Chụp màn hình", style = MaterialTheme.typography.titleLarge)
                                Text(captureStatus)
                                Text(frameStatus, style = MaterialTheme.typography.bodySmall)
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

                        com.soulknight.matrixai.capture.MatrixDatasetPanel()

                        com.soulknight.matrixai.vision.PlayerAutoDetectionPanel()

                        Card {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("6. Hiệu chỉnh tốc độ di chuyển", style = MaterialTheme.typography.titleLarge)
                                Text("Chạm vào tâm nhân vật trên ảnh xem trước. Tọa độ này thuộc ảnh đã thu nhỏ, không phải tọa độ màn hình.")
                                val currentFrame = previewFrame
                                if (currentFrame != null) {
                                    PlayerSelectionPreview(currentFrame) { x, y ->
                                        playerCenterX = x.toString()
                                        playerCenterY = y.toString()
                                    }
                                } else {
                                    Text("Chưa có ảnh xem trước. Hãy cấp quyền chụp màn hình trước.")
                                }
                                Text("Tâm nhân vật trong ảnh: X=$playerCenterX, Y=$playerCenterY")
                                Text("Nhập tâm joystick theo pixel màn hình thật của điện thoại:")
                                CoordinateFields("Tâm joystick", joystickCenterX, joystickCenterY,
                                    { joystickCenterX = it }, { joystickCenterY = it })
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = safeCalibrationArea, onCheckedChange = { safeCalibrationArea = it })
                                    Text("Tôi đang ở khu vực trống, không sát tường hoặc cửa.")
                                }
                                Text(movementStatus, style = MaterialTheme.typography.bodySmall)
                                if (movementProfile.confidence > 0f) {
                                    Text(
                                        "Tốc độ (pixel ảnh/giây): ngang %.1f · dọc %.1f · chéo %.1f · độ tin cậy %.0f%%".format(
                                            movementProfile.horizontalPixelsPerSecond,
                                            movementProfile.verticalPixelsPerSecond,
                                            movementProfile.diagonalPixelsPerSecond,
                                            movementProfile.confidence * 100f
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    enabled = previewFrame != null && accessibilityConnected && safeCalibrationArea,
                                    onClick = {
                                        val px = playerCenterX.toIntOrNull()
                                        val py = playerCenterY.toIntOrNull()
                                        val jx = joystickCenterX.toFloatOrNull()
                                        val jy = joystickCenterY.toFloatOrNull()
                                        if (px == null || py == null || jx == null || jy == null) {
                                            movementStatus = "Hãy chọn tâm nhân vật và nhập tọa độ joystick hợp lệ."
                                        } else {
                                            val started = calibrationRunner.start(px, py, InputTarget(jx, jy))
                                            if (!started && movementStatus == "Chưa hiệu chỉnh tốc độ") {
                                                movementStatus = "Không thể bắt đầu hiệu chỉnh. Kiểm tra quyền và tọa độ."
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Quét tốc độ di chuyển") }
                                OutlinedButton(
                                    onClick = { calibrationRunner.stop() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Dừng hiệu chỉnh") }
                                Divider()
                                Text("Theo dõi buff tốc độ", style = MaterialTheme.typography.titleMedium)
                                Text(adaptiveStatus, style = MaterialTheme.typography.bodySmall)
                                Button(
                                    enabled = previewFrame != null && movementProfile.confidence > 0f &&
                                        playerCenterX.toIntOrNull() != null && playerCenterY.toIntOrNull() != null &&
                                        adaptiveSession == null,
                                    onClick = {
                                        val px = playerCenterX.toIntOrNull()
                                        val py = playerCenterY.toIntOrNull()
                                        if (px == null || py == null) {
                                            adaptiveStatus = "Hãy chạm chọn tâm nhân vật trên ảnh xem trước."
                                        } else {
                                            val session = MovementAdaptiveSession(
                                                initialProfile = movementProfile,
                                                playerCenterX = px,
                                                playerCenterY = py,
                                                onProfileChanged = { updated, _ ->
                                                    mainHandler.post {
                                                        movementProfile = updated
                                                        movementProfileStore.save(updated)
                                                    }
                                                },
                                                onStatus = { status ->
                                                    mainHandler.post { adaptiveStatus = status }
                                                }
                                            )
                                            adaptiveSession = session
                                            if (!session.start()) adaptiveSession = null
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Bật theo dõi buff tự động") }
                                OutlinedButton(
                                    enabled = adaptiveSession != null,
                                    onClick = {
                                        adaptiveSession?.stop()
                                        adaptiveSession = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Dừng theo dõi buff") }
                                Text(
                                    "Chế độ này không tự chạm joystick. AI theo dõi chuyển động tự nhiên khi bạn chơi, so sánh tốc độ đo được với hồ sơ đã hiệu chỉnh và lưu lại khi phát hiện thay đổi đủ lớn. Vật cản, hiệu ứng hình ảnh hoặc mất dấu có thể làm phép đo sai; hãy dừng nếu nhân vật không được theo dõi chính xác.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "AI thực hiện 3 lượt joystick ngắn, lần lượt ngang, dọc và chéo. Tốc độ được đo trên ảnh chụp khoảng 10 FPS; nếu nhân vật không di chuyển rõ, phép đo sẽ bị từ chối.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
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
private fun PlayerSelectionPreview(frame: FrameSnapshot, onSelected: (Int, Int) -> Unit) {
    val bitmap = remember(frame) {
        Bitmap.createBitmap(frame.pixels, frame.width, frame.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Ảnh chụp màn hình game; chạm vào tâm nhân vật để chọn vị trí theo dõi",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(220.dp).pointerInput(frame) {
            detectTapGestures { offset ->
                val scale = min(size.width.toFloat() / frame.width, size.height.toFloat() / frame.height)
                val renderedWidth = frame.width * scale
                val renderedHeight = frame.height * scale
                val left = (size.width - renderedWidth) / 2f
                val top = (size.height - renderedHeight) / 2f
                if (offset.x in left..(left + renderedWidth) && offset.y in top..(top + renderedHeight)) {
                    val x = ((offset.x - left) / scale).toInt().coerceIn(0, frame.width - 1)
                    val y = ((offset.y - top) / scale).toInt().coerceIn(0, frame.height - 1)
                    onSelected(x, y)
                }
            }
        }
    )
}

@Composable
private fun CoordinateFields(title: String, x: String, y: String, onX: (String) -> Unit, onY: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = x, onValueChange = onX, label = { Text("X") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = y, onValueChange = onY, label = { Text("Y") }, modifier = Modifier.weight(1f), singleLine = true)
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
