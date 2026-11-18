package com.macro.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.accessibility.AccessibilityEvent
import com.macro.app.data.MacroDatabase
import com.macro.app.data.MacroRepository
import com.macro.app.engine.MacroEngine
import com.macro.app.model.MacroState
import com.macro.app.utils.CoordinateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.hypot

class MacroAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayRoot: FrameLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var captureView: View? = null
    private var pauseButton: TextView? = null
    private lateinit var windowManager: WindowManager
    private lateinit var repository: MacroRepository
    private var forwardingGesture = false

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downTime = 0L
    private var multiTouch = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = MacroRepository(MacroDatabase.getInstance(applicationContext).macroDao())
        MacroEngine.init(applicationContext)
        MacroEngine.setAccessibilityConnected(true)
    }

    /**
     * Inject one short tap at screen coordinates after the floating button is released.
     * This does not install a full-screen touch-capture window.
     */
    fun dispatchTapAt(
        screenX: Float,
        screenY: Float,
        onFinished: () -> Unit
    ): Boolean {
        val path = Path().apply {
            moveTo(screenX, screenY)
            lineTo(screenX + 1f, screenY + 1f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 55L))
            .build()
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onFinished()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onFinished()
            }
        }
        return runCatching { dispatchGesture(gesture, callback, null) }.getOrDefault(false)
    }

    /**
     * Displays a transparent touch-capture layer over the foreground app.
     * Captured single-finger gestures are recorded and replayed to the app below
     * using AccessibilityService.dispatchGesture().
     */
    fun startTouchRecordingOverlay(): Boolean {
        if (overlayRoot != null) return true
        if (!::windowManager.isInitialized || !::repository.isInitialized) return false

        MacroEngine.emergencyStop()
        MacroEngine.recorder.startRecording()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val capture = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event -> handleCapturedTouch(event) }
        }
        captureView = capture
        root.addView(
            capture,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // The control strip is the only visible part of the overlay. The rest
        // remains transparent so the user can see the app being recorded.
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(7), dp(9), dp(7))
            background = roundedBackground(Color.argb(224, 20, 23, 31), Color.rgb(255, 46, 76))
        }
        val title = TextView(this).apply {
            text = "● TOUCH REC"
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(dp(3), 0, dp(7), 0)
        }
        pauseButton = makeOverlayButton("TẠM DỪNG") {
            when (MacroEngine.recorder.recorderState.value) {
                MacroState.RECORDING -> {
                    MacroEngine.recorder.pauseRecording()
                    pauseButton?.text = "TIẾP TỤC"
                }
                MacroState.PAUSED -> {
                    MacroEngine.recorder.resumeRecording()
                    pauseButton?.text = "TẠM DỪNG"
                }
                else -> Unit
            }
        }
        val save = makeOverlayButton("LƯU") { finishTouchRecordingAndSave() }
        val cancel = makeOverlayButton("HỦY") {
            MacroEngine.recorder.cancelRecording()
            removeTouchRecordingOverlay()
            Toast.makeText(this, "Đã hủy ghi thao tác", Toast.LENGTH_SHORT).show()
        }
        controls.addView(title)
        controls.addView(pauseButton)
        controls.addView(save)
        controls.addView(cancel)

        root.addView(
            controls,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply { topMargin = dp(26) }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        return try {
            overlayRoot = root
            overlayParams = params
            windowManager.addView(root, params)
            Toast.makeText(
                this,
                "Touch Recording đã bật. Chạm hoặc vuốt trên ứng dụng bên dưới.",
                Toast.LENGTH_LONG
            ).show()
            true
        } catch (error: Exception) {
            overlayRoot = null
            overlayParams = null
            captureView = null
            MacroEngine.recorder.cancelRecording()
            Toast.makeText(
                this,
                "Không thể mở lớp phủ ghi chạm: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun handleCapturedTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
                downTime = event.eventTime
                multiTouch = false
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                multiTouch = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                lastX = event.rawX
                lastY = event.rawY
                if (!multiTouch && !forwardingGesture &&
                    MacroEngine.recorder.recorderState.value == MacroState.RECORDING
                ) {
                    val duration = (event.eventTime - downTime).coerceAtLeast(1L)
                    val distance = hypot(lastX - downX, lastY - downY)
                    val isSwipe = distance > dp(18)
                    val (startNX, startNY) = CoordinateConverter.toNormalized(this, downX, downY)
                    val (endNX, endNY) = CoordinateConverter.toNormalized(this, lastX, lastY)

                    when {
                        isSwipe -> MacroEngine.recorder.recordSwipe(
                            startNX, startNY, endNX, endNY, duration.coerceAtLeast(60L)
                        )
                        duration >= 500L -> MacroEngine.recorder.recordLongPress(
                            startNX, startNY, duration
                        )
                        else -> MacroEngine.recorder.recordTap(
                            startNX, startNY, duration.coerceAtLeast(35L)
                        )
                    }
                    forwardGesture(downX, downY, lastX, lastY, duration, isSwipe)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                multiTouch = false
                return true
            }
        }
        return true
    }

    private fun forwardGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        isSwipe: Boolean
    ) {
        val params = overlayParams ?: return
        val root = overlayRoot ?: return
        if (forwardingGesture) return
        forwardingGesture = true

        // Temporarily make the overlay non-touchable so the injected gesture
        // reaches the underlying application rather than the capture layer.
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            windowManager.updateViewLayout(root, params)
        } catch (_: Exception) {
            forwardingGesture = false
            return
        }

        val path = Path().apply {
            moveTo(startX, startY)
            if (isSwipe) lineTo(endX, endY) else lineTo(startX + 1f, startY + 1f)
        }
        val gestureDuration = durationMs.coerceIn(40L, 10_000L)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, gestureDuration))
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    restoreOverlayTouchability()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    restoreOverlayTouchability()
                }
            },
            null
        )
        if (!accepted) restoreOverlayTouchability()
    }

    private fun restoreOverlayTouchability() {
        val params = overlayParams
        val root = overlayRoot
        if (params != null && root != null) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            runCatching { windowManager.updateViewLayout(root, params) }
        }
        forwardingGesture = false
    }

    fun finishTouchRecordingAndSave() {
        if (overlayRoot == null) return
        if (MacroEngine.recorder.recorderState.value == MacroState.PAUSED) {
            MacroEngine.recorder.resumeRecording()
        }
        val macro = MacroEngine.recorder.finishAndBuildMacro("Game Macro")
        removeTouchRecordingOverlay()
        if (macro.actions.isEmpty()) {
            Toast.makeText(this, "Chưa ghi được thao tác nào", Toast.LENGTH_SHORT).show()
            return
        }
        serviceScope.launch {
            runCatching {
                repository.saveMacro(macro)
                MacroEngine.selectMacro(macro)
            }.onSuccess {
                Toast.makeText(
                    this@MacroAccessibilityService,
                    "Đã lưu ${macro.name}: ${macro.actions.size} thao tác",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Toast.makeText(
                    this@MacroAccessibilityService,
                    "Không thể lưu Macro: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun stopTouchRecordingOverlay() {
        removeTouchRecordingOverlay()
    }

    private fun removeTouchRecordingOverlay() {
        overlayRoot?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayRoot = null
        overlayParams = null
        captureView = null
        pauseButton = null
        forwardingGesture = false
    }

    private fun makeOverlayButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = roundedBackground(Color.rgb(54, 60, 74), Color.TRANSPARENT)
            setOnClickListener { action() }
        }

    private fun roundedBackground(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(9).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Does not inspect screen text, passwords, or private view hierarchies.
    }

    override fun onInterrupt() {
        MacroEngine.emergencyStop("AccessibilityService bị gián đoạn bởi hệ thống.")
        removeTouchRecordingOverlay()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        removeTouchRecordingOverlay()
        MacroEngine.setAccessibilityConnected(false)
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        removeTouchRecordingOverlay()
        serviceScope.cancel()
        MacroEngine.setAccessibilityConnected(false)
        instance = null
        super.onDestroy()
    }
}
