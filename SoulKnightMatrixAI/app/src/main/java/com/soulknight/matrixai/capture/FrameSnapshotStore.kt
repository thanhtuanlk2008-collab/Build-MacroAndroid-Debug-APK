package com.soulknight.matrixai.capture

import com.soulknight.matrixai.vision.FrameSnapshot
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process, read-only stream of the latest user-approved screen frame.
 * Consumers should avoid retaining old frames because each frame owns an IntArray.
 */
object FrameSnapshotStore {
    @Volatile
    var latestFrame: FrameSnapshot? = null
        private set

    private val listeners = CopyOnWriteArrayList<(FrameSnapshot) -> Unit>()

    fun addListener(listener: (FrameSnapshot) -> Unit) {
        listeners.addIfAbsent(listener)
        latestFrame?.let { frame -> runCatching { listener(frame) } }
    }

    fun removeListener(listener: (FrameSnapshot) -> Unit) {
        listeners.remove(listener)
    }

    fun publish(frame: FrameSnapshot) {
        latestFrame = frame
        listeners.forEach { listener ->
            runCatching { listener(frame) }
        }
    }

    fun clear() {
        latestFrame = null
    }
}
