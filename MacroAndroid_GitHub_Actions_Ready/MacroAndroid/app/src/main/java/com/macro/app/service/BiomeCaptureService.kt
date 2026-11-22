package com.macro.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.macro.app.MainActivity
import com.macro.app.R
import com.macro.app.biome.BiomeClassifier
import com.macro.app.biome.BiomeFeatureExtractor
import com.macro.app.biome.BiomeFeedbackRepository
import com.macro.app.biome.BiomeRecognitionEngine
import com.macro.app.data.MacroDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Explicit, user-approved screen capture for local biome analysis.
 * Captures are processed in memory; only feature vectors/predictions are persisted.
 */
class BiomeCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processing = AtomicBoolean(false)
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var lastAnalysisAt = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopCapture()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        if (projection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(projectionCallback, Handler(mainLooper))
            beginCapture()
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start screen capture", error)
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "biome_capture"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Biome screen capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stopIntent = PendingIntent.getService(
            this, 21, Intent(this, BiomeCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Soul Knight Matrix AI")
            .setContentText("Đang phân tích màn hình theo yêu cầu của bạn")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Dừng", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning = true
    }

    private fun beginCapture() {
        val metrics = if (Build.VERSION.SDK_INT >= 30) {
            val windowManager = getSystemService(WindowManager::class.java)
            windowManager.currentWindowMetrics.bounds.let {
                Pair(it.width(), it.height())
            }
        } else {
            @Suppress("DEPRECATION")
            resources.displayMetrics.let { Pair(it.widthPixels, it.heightPixels) }
        }
        val width = metrics.first.coerceAtLeast(1)
        val height = metrics.second.coerceAtLeast(1)
        val density = resources.displayMetrics.densityDpi

        captureThread = HandlerThread("BiomeFrameCapture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader -> analyzeLatestFrame(reader) }, captureHandler)

        virtualDisplay = projection?.createVirtualDisplay(
            "SoulKnightBiomeCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            captureHandler
        )
        if (virtualDisplay == null) {
            throw IllegalStateException("VirtualDisplay could not be created")
        }
    }

    private fun analyzeLatestFrame(reader: ImageReader) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisAt < ANALYSIS_INTERVAL_MS || !processing.compareAndSet(false, true)) {
            val stale = reader.acquireLatestImage()
            stale?.close()
            return
        }
        lastAnalysisAt = now

        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: run {
                processing.set(false)
                return
            }
            val bitmap = imageToBitmap(image)
            image.close()
            image = null

            serviceScope.launch {
                try {
                    val database = MacroDatabase.getInstance(this@BiomeCaptureService)
                    val feedback = BiomeFeedbackRepository(database.biomeFeedbackDao())
                    val engine = BiomeRecognitionEngine(
                        BiomeFeatureExtractor(),
                        BiomeClassifier(),
                        feedback
                    )
                    engine.analyze(bitmap)
                } catch (error: Exception) {
                    Log.e(TAG, "Biome frame analysis failed", error)
                } finally {
                    bitmap.recycle()
                    processing.set(false)
                }
            }
        } catch (error: Exception) {
            image?.close()
            processing.set(false)
            Log.e(TAG, "Could not read captured frame", error)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val paddedWidth = rowStride / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }

    private fun stopCapture() {
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { projection?.unregisterCallback(projectionCallback) }
        runCatching { projection?.stop() }
        projection = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        isRunning = false
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BiomeCaptureService"
        private const val NOTIFICATION_ID = 3107
        private const val ANALYSIS_INTERVAL_MS = 2000L
        const val ACTION_STOP = "com.macro.app.action.STOP_BIOME_CAPTURE"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun stop(context: Context) {
            context.startService(Intent(context, BiomeCaptureService::class.java).setAction(ACTION_STOP))
        }
    }
}
