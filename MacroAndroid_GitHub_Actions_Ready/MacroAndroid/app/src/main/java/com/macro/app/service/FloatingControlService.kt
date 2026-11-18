package com.macro.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.macro.app.MainActivity
import com.macro.app.data.MacroDatabase
import com.macro.app.data.MacroRepository
import com.macro.app.data.SettingsDataStore
import com.macro.app.engine.MacroEngine
import com.macro.app.engine.SpeedEngine
import com.macro.app.model.MacroState
import com.macro.app.utils.CoordinateConverter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingControlService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.macro.app.action.START_FLOATING"
        const val ACTION_STOP_SERVICE = "com.macro.app.action.STOP_FLOATING"
        const val ACTION_EMERGENCY_STOP_MACRO = "com.macro.app.action.EMERGENCY_STOP_MACRO"
        private const val CHANNEL_ID = "macro_floating_channel"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, FloatingControlService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingControlService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: MacroRepository

    private var controlView: View? = null
    private var controlParams: WindowManager.LayoutParams? = null

    private var targetPinView: View? = null
    private var targetPinParams: WindowManager.LayoutParams? = null

    private var isCollapsed = false
    private var panelAlpha = 0.88f
    private var panelScale = 1.0f

    // UI references for live updates
    private var expandedContainer: LinearLayout? = null
    private var collapsedPill: TextView? = null
    private var tvSpeedBadge: TextView? = null
    private var tvStatusBadge: TextView? = null
    private var btnPlayPause: TextView? = null
    private var btnRec: TextView? = null
    private var recControlsRow: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        MacroEngine.init(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsDataStore = SettingsDataStore(applicationContext)
        repository = MacroRepository(MacroDatabase.getInstance(applicationContext).macroDao())

        createNotificationChannel()
        startForegroundCompat()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        lifecycleScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            panelAlpha = settings.floatingAlpha
            panelScale = settings.floatingScale
            showFloatingPanel(settings.floatingPosX, settings.floatingPosY)
            observeEngineState()
            observeSettings()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                MacroEngine.emergencyStop()
                stopSelf()
            }
            ACTION_EMERGENCY_STOP_MACRO -> {
                MacroEngine.emergencyStop("Dừng khẩn cấp từ thanh thông báo.")
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MacroEngine Floating Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Điều khiển Macro nổi và nút dừng khẩn cấp"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val emergencyStopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingControlService::class.java).apply {
                action = ACTION_EMERGENCY_STOP_MACRO
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val closeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingControlService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MacroEngine Pro đang hoạt động")
            .setContentText("Bảng điều khiển nổi sẵn sàng • Chạm Dừng khẩn cấp nếu cần")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Dừng khẩn cấp", emergencyStopIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tắt bảng nổi", closeIntent)
            .build()
    }

    private fun overlayLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingPanel(startX: Int, startY: Int) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = panelAlpha
            scaleX = panelScale
            scaleY = panelScale
        }

        // Collapsed Pill
        val pill = TextView(this).apply {
            text = "⚡ MACRO"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = makeCardBg(Color.parseColor("#E614161D"), Color.parseColor("#FF2E4C"), 20f)
            visibility = View.GONE
            setOnClickListener { toggleCollapse(false) }
        }
        collapsedPill = pill

        // Expanded REDMAGIC-inspired Gaming Panel
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = makeCardBg(Color.parseColor("#EC101218"), Color.parseColor("#FF2E4C"), 14f)
        }
        expandedContainer = panel

        // Header (Drag Handle + Status + Hide + Settings)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val statusBadge = TextView(this).apply {
            text = "● IDLE"
            setTextColor(Color.parseColor("#9AA3B2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, dp(8), 0)
        }
        tvStatusBadge = statusBadge

        val btnHide = makeMiniBtn("—", "#252936") { toggleCollapse(true) }
        val btnSettings = makeMiniBtn("⚙", "#252936") {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        val btnClose = makeMiniBtn("✕", "#3A1C24") {
            MacroEngine.emergencyStop()
            stopSelf()
        }

        header.addView(statusBadge)
        header.addView(btnHide)
        header.addView(space(4))
        header.addView(btnSettings)
        header.addView(space(4))
        header.addView(btnClose)
        panel.addView(header)

        // Speed Row: MACRO SPEED: 1.0x  [-] [+]
        val speedRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val speedText = TextView(this).apply {
            text = "MACRO SPEED: 1.0x"
            setTextColor(Color.parseColor("#FF4D67"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, dp(8), 0)
        }
        tvSpeedBadge = speedText
        val btnSpeedDown = makeMiniBtn("-", "#202430") { SpeedEngine.stepSpeedDown() }
        val btnSpeedUp = makeMiniBtn("+", "#202430") { SpeedEngine.stepSpeedUp() }

        speedRow.addView(speedText)
        speedRow.addView(btnSpeedDown)
        speedRow.addView(space(4))
        speedRow.addView(btnSpeedUp)
        panel.addView(speedRow)

        // Main Action Row: REC | PLAY/PAUSE | STOP
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val recBtn = makeActionBtn("REC", "#B71C1C") { handleRecClick() }
        btnRec = recBtn
        val playPauseBtn = makeActionBtn("PLAY", "#1B5E20") { handlePlayPauseClick() }
        btnPlayPause = playPauseBtn
        val stopBtn = makeActionBtn("STOP", "#37474F") {
            MacroEngine.emergencyStop()
            if (MacroEngine.recorder.recorderState.value != MacroState.IDLE) {
                MacroEngine.recorder.cancelRecording()
                hideTargetPin()
                MacroAccessibilityService.instance?.stopTouchRecordingOverlay()
            }
        }

        actionRow.addView(recBtn)
        actionRow.addView(space(5))
        actionRow.addView(playPauseBtn)
        actionRow.addView(space(5))
        actionRow.addView(stopBtn)
        panel.addView(actionRow)

        // Recording Capture Row (Visible only when Target Pin Recorder is active)
        val recRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
            visibility = View.GONE
        }
        recRow.addView(makeActionBtn("LƯU", "#2E7D32") {
            MacroAccessibilityService.instance?.finishTouchRecordingAndSave()
                ?: Toast.makeText(this, "Accessibility Service chưa sẵn sàng.", Toast.LENGTH_SHORT).show()
        })
        recControlsRow = recRow
        panel.addView(recRow)

        root.addView(pill)
        root.addView(panel)

        // Dragging support on root
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 6 || abs(dy) > 6) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(root, params)
                    }
                    isDragging
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        lifecycleScope.launch {
                            settingsDataStore.updateFloatingPosition(params.x, params.y)
                        }
                    }
                    isDragging
                }
                else -> false
            }
        }

        pill.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        lifecycleScope.launch {
                            settingsDataStore.updateFloatingPosition(params.x, params.y)
                        }
                    } else {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        controlView = root
        controlParams = params
        windowManager.addView(root, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showTargetPin() {
        if (targetPinView != null) return
        val bounds = CoordinateConverter.getScreenBounds(this)
        val params = WindowManager.LayoutParams(
            dp(44),
            dp(44),
            overlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.widthPx / 2 - dp(22)
            y = bounds.heightPx / 2 - dp(22)
        }

        val pin = TextView(this).apply {
            text = "⊕"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF2E4C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            background = makeCardBg(Color.parseColor("#66000000"), Color.parseColor("#00E5FF"), 22f)
        }

        var ix = 0
        var iy = 0
        var tx = 0f
        var ty = 0f
        pin.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ix = params.x
                    iy = params.y
                    tx = event.rawX
                    ty = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = ix + (event.rawX - tx).toInt()
                    params.y = iy + (event.rawY - ty).toInt()
                    windowManager.updateViewLayout(pin, params)
                    true
                }
                else -> false
            }
        }

        targetPinView = pin
        targetPinParams = params
        windowManager.addView(pin, params)
    }

    private fun hideTargetPin() {
        targetPinView?.let {
            runCatching { windowManager.removeView(it) }
        }
        targetPinView = null
        targetPinParams = null
    }

    private fun capturePinTap(isLongPress: Boolean) {
        val pinParams = targetPinParams ?: return
        val centerX = pinParams.x + dp(22).toFloat()
        val centerY = pinParams.y + dp(22).toFloat()
        val (nx, ny) = CoordinateConverter.toNormalized(this, centerX, centerY)
        if (isLongPress) {
            MacroEngine.recorder.recordLongPress(nx, ny, 800L)
            Toast.makeText(this, "Đã ghi Nhấn giữ tại tâm ngắm", Toast.LENGTH_SHORT).show()
        } else {
            MacroEngine.recorder.recordTap(nx, ny, 45L)
            Toast.makeText(this, "Đã ghi Chạm tại tâm ngắm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishAndSaveRecording() {
        val macro = MacroEngine.recorder.finishAndBuildMacro("Game Macro")
        hideTargetPin()
        if (macro.actions.isEmpty()) {
            Toast.makeText(this, "Chưa có thao tác nào được ghi", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            repository.saveMacro(macro)
            MacroEngine.selectMacro(macro)
            Toast.makeText(this@FloatingControlService, "Đã lưu ${macro.name} (${macro.actions.size} bước)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRecClick() {
        when (MacroEngine.recorder.recorderState.value) {
            MacroState.IDLE, MacroState.STOPPED -> {
                val accessibility = MacroAccessibilityService.instance
                if (accessibility == null) {
                    Toast.makeText(
                        this,
                        "Hãy bật MacroAndroid trong Cài đặt Trợ năng trước.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                if (!accessibility.startTouchRecordingOverlay()) {
                    Toast.makeText(this, "Không thể khởi động Touch Recording.", Toast.LENGTH_LONG).show()
                }
            }
            MacroState.RECORDING -> {
                MacroEngine.recorder.pauseRecording()
            }
            MacroState.PAUSED -> {
                MacroEngine.recorder.resumeRecording()
            }
            else -> Unit
        }
    }

    private fun handlePlayPauseClick() {
        val telemetry = MacroEngine.getTelemetryFlow()?.value
        when (telemetry?.state) {
            MacroState.PLAYING -> MacroEngine.pause()
            MacroState.PAUSED -> MacroEngine.resume()
            else -> {
                val selected = MacroEngine.selectedMacro.value
                if (selected != null) {
                    MacroEngine.play(selected)
                } else {
                    lifecycleScope.launch {
                        val firstMacro = repository.getAllMacrosOnce().firstOrNull()
                        if (firstMacro != null) {
                            MacroEngine.play(firstMacro)
                        } else {
                            Toast.makeText(
                                this@FloatingControlService,
                                "Chưa có Macro. Hãy tạo hoặc ghi Macro trước.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun toggleCollapse(collapse: Boolean) {
        isCollapsed = collapse
        expandedContainer?.visibility = if (collapse) View.GONE else View.VISIBLE
        collapsedPill?.visibility = if (collapse) View.VISIBLE else View.GONE
    }

    private fun observeEngineState() {
        lifecycleScope.launch {
            SpeedEngine.speedMultiplier.collectLatest { speed ->
                tvSpeedBadge?.text = String.format("MACRO SPEED: %.2fx", speed).replace(".00x", ".0x")
            }
        }

        lifecycleScope.launch {
            MacroEngine.recorder.recorderState.collectLatest { recState ->
                val isRec = recState == MacroState.RECORDING || recState == MacroState.PAUSED
                recControlsRow?.visibility = if (isRec) View.VISIBLE else View.GONE
                btnRec?.text = when (recState) {
                    MacroState.RECORDING -> "PAUSE REC"
                    MacroState.PAUSED -> "RESUME REC"
                    else -> "REC"
                }
            }
        }

        lifecycleScope.launch {
            MacroEngine.getTelemetryFlow()?.collectLatest { telemetry ->
                val statusText = when (telemetry.state) {
                    MacroState.PLAYING -> "● PLAYING (${telemetry.currentLoop}/${if (telemetry.targetLoops == -1) "∞" else telemetry.targetLoops})"
                    MacroState.PAUSED -> "❚❚ PAUSED"
                    MacroState.ERROR -> "⚠ ERROR"
                    else -> "● IDLE"
                }
                tvStatusBadge?.text = statusText
                tvStatusBadge?.setTextColor(
                    when (telemetry.state) {
                        MacroState.PLAYING -> Color.parseColor("#00E676")
                        MacroState.PAUSED -> Color.parseColor("#FFC107")
                        MacroState.ERROR -> Color.parseColor("#FF5252")
                        else -> Color.parseColor("#9AA3B2")
                    }
                )
                btnPlayPause?.text = when (telemetry.state) {
                    MacroState.PLAYING -> "PAUSE"
                    MacroState.PAUSED -> "RESUME"
                    else -> "PLAY"
                }
            }
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            settingsDataStore.settingsFlow.collectLatest { settings ->
                panelAlpha = settings.floatingAlpha
                panelScale = settings.floatingScale
                controlView?.apply {
                    alpha = panelAlpha
                    scaleX = panelScale
                    scaleY = panelScale
                }
            }
        }
    }

    private fun makeCardBg(fillColor: Int, strokeColor: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radiusDp.toInt()).toFloat()
        }
    }

    private fun makeMiniBtn(label: String, bgHex: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = makeCardBg(Color.parseColor(bgHex), Color.parseColor("#3B4254"), 6f)
            setOnClickListener { onClick() }
        }
    }

    private fun makeActionBtn(label: String, bgHex: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = makeCardBg(Color.parseColor(bgHex), Color.parseColor("#525A6E"), 8f)
            setOnClickListener { onClick() }
        }
    }

    private fun space(dpWidth: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(dpWidth), 1)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        isRunning = false
        hideTargetPin()
        controlView?.let { runCatching { windowManager.removeView(it) } }
        controlView = null
        super.onDestroy()
    }
}
