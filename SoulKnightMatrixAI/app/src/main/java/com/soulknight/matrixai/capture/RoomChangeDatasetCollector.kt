package com.soulknight.matrixai.capture

import android.content.Context
import android.graphics.Bitmap
import com.soulknight.matrixai.vision.FrameSnapshot
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Saves one representative frame after a persistent, large visual transition.
 * This is a visual-change heuristic, not a semantic room detector.
 */
class RoomChangeDatasetCollector(
    context: Context,
    private val onSaved: (File) -> Unit,
    private val onStatus: (String) -> Unit = {}
) {
    private val directory = File(context.filesDir, "matrix-dataset").apply { mkdirs() }
    private var previousSignature: IntArray? = null
    private var candidateSignature: IntArray? = null
    private var candidateFrame: FrameSnapshot? = null
    private var candidateCount = 0
    private var lastSavedAt = 0L
    private var enabled = false

    private val listener: (FrameSnapshot) -> Unit = { frame -> process(frame) }

    @Synchronized
    fun start() {
        if (enabled) return
        enabled = true
        previousSignature = null
        candidateSignature = null
        candidateFrame = null
        candidateCount = 0
        FrameSnapshotStore.addListener(listener)
        onStatus("Đang theo dõi thay đổi hình ảnh phòng")
    }

    @Synchronized
    fun stop() {
        if (!enabled) return
        enabled = false
        FrameSnapshotStore.removeListener(listener)
        candidateFrame = null
        candidateSignature = null
        candidateCount = 0
        onStatus("Đã dừng thu thập ảnh")
    }

    @Synchronized
    private fun process(frame: FrameSnapshot) {
        if (!enabled || frame.width < 16 || frame.height < 16) return
        val signature = signature(frame)
        val baseline = previousSignature
        if (baseline == null) {
            previousSignature = signature
            return
        }
        val change = difference(baseline, signature)
        if (change >= CHANGE_THRESHOLD) {
            val candidate = candidateSignature
            if (candidate != null && difference(candidate, signature) <= STABILITY_THRESHOLD) {
                candidateCount++
            } else {
                candidateSignature = signature
                candidateFrame = frame
                candidateCount = 1
            }
            if (candidateCount >= REQUIRED_STABLE_FRAMES &&
                frame.timestampMillis - lastSavedAt >= SAVE_COOLDOWN_MS) {
                val selected = candidateFrame ?: frame
                saveFrame(selected)
                lastSavedAt = frame.timestampMillis
                previousSignature = signature
                candidateSignature = null
                candidateFrame = null
                candidateCount = 0
            }
        } else {
            previousSignature = signature
            candidateSignature = null
            candidateFrame = null
            candidateCount = 0
        }
    }

    private fun signature(frame: FrameSnapshot): IntArray {
        val cols = 24
        val rows = 14
        val result = IntArray(cols * rows)
        for (gy in 0 until rows) {
            for (gx in 0 until cols) {
                val x = ((gx + 0.5f) * frame.width / cols).toInt().coerceIn(0, frame.width - 1)
                val y = ((gy + 0.5f) * frame.height / rows).toInt().coerceIn(0, frame.height - 1)
                val color = frame.pixels[y * frame.width + x]
                result[gy * cols + gx] = (((color shr 16) and 0xff) / 32 shl 6) or
                    (((color shr 8) and 0xff) / 32 shl 3) or ((color and 0xff) / 32)
            }
        }
        return result
    }

    private fun difference(a: IntArray, b: IntArray): Float {
        if (a.size != b.size || a.isEmpty()) return 1f
        var changed = 0
        for (i in a.indices) if (abs(a[i] - b[i]) >= 2) changed++
        return changed.toFloat() / a.size
    }

    private fun saveFrame(frame: FrameSnapshot) {
        runCatching {
            val bitmap = Bitmap.createBitmap(frame.pixels, frame.width, frame.height, Bitmap.Config.ARGB_8888)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(directory, "room_change_$stamp.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bitmap.recycle()
            onSaved(file)
            onStatus("Đã lưu ảnh thay đổi phòng: ${file.name}")
        }.onFailure { onStatus("Không lưu được ảnh: ${it.message}") }
    }

    fun savedFiles(): List<File> = directory.listFiles { f -> f.isFile && f.extension == "jpg" }
        ?.sortedBy { it.name }.orEmpty()

    companion object {
        private const val CHANGE_THRESHOLD = 0.42f
        private const val STABILITY_THRESHOLD = 0.16f
        private const val REQUIRED_STABLE_FRAMES = 3
        private const val SAVE_COOLDOWN_MS = 2500L
    }
}
