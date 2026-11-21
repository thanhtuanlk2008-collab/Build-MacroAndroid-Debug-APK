package com.soulknight.matrixai.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import android.hardware.display.DisplayManager
import androidx.core.app.NotificationCompat
import com.soulknight.matrixai.vision.FrameSnapshot
import kotlin.math.max
import kotlin.math.roundToInt

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var lastFrameAtMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_START) {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }
            if (data == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, null)
            startCapture()
        }
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        captureThread = HandlerThread("MatrixFrameProcessor").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)
        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val now = SystemClock.elapsedRealtime()
                if (now - lastFrameAtMillis >= FRAME_INTERVAL_MS) {
                    lastFrameAtMillis = now
                    imageToSnapshot(image)?.let(FrameSnapshotStore::publish)
                }
            } finally {
                image.close()
            }
        }, captureHandler)
        virtualDisplay = projection?.createVirtualDisplay(
            "SoulKnightMatrixAI-Capture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            captureHandler
        )
    }

    /** Converts RGBA_8888 into a bounded-size ARGB frame, respecting row/pixel stride. */
    private fun imageToSnapshot(image: Image): FrameSnapshot? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride < 4 || rowStride <= 0) return null

        val scale = max(1, (image.width + MAX_FRAME_WIDTH - 1) / MAX_FRAME_WIDTH)
        val outWidth = (image.width.toFloat() / scale).roundToInt().coerceAtLeast(1)
        val outHeight = (image.height.toFloat() / scale).roundToInt().coerceAtLeast(1)
        val pixels = IntArray(outWidth * outHeight)
        val base = buffer.position()

        for (outY in 0 until outHeight) {
            val sourceY = (outY * scale).coerceAtMost(image.height - 1)
            for (outX in 0 until outWidth) {
                val sourceX = (outX * scale).coerceAtMost(image.width - 1)
                val index = base + sourceY * rowStride + sourceX * pixelStride
                if (index < 0 || index + 3 >= buffer.limit()) return null
                val red = buffer.get(index).toInt() and 0xff
                val green = buffer.get(index + 1).toInt() and 0xff
                val blue = buffer.get(index + 2).toInt() and 0xff
                val alpha = buffer.get(index + 3).toInt() and 0xff
                pixels[outY * outWidth + outX] =
                    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        return FrameSnapshot(outWidth, outHeight, SystemClock.elapsedRealtime(), pixels)
    }

    private fun buildNotification(): Notification {
        val channelId = "matrix_ai_capture"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Matrix AI screen capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Soul Knight Matrix AI")
            .setContentText("Đang truyền khung hình đã được cấp quyền vào bộ phân tích")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        projection?.stop()
        projection = null
        FrameSnapshotStore.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.soulknight.matrixai.capture.START"
        const val ACTION_STOP = "com.soulknight.matrixai.capture.STOP"
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        private const val NOTIFICATION_ID = 4101
        private const val MAX_FRAME_WIDTH = 480
        private const val FRAME_INTERVAL_MS = 200L
    }
}
