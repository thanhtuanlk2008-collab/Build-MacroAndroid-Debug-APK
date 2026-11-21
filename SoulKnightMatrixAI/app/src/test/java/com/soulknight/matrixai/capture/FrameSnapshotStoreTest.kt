package com.soulknight.matrixai.capture

import com.soulknight.matrixai.vision.FrameSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FrameSnapshotStoreTest {
    @After
    fun cleanUp() {
        FrameSnapshotStore.clear()
    }

    @Test
    fun publishesLatestFrameAndNotifiesListener() {
        val frame = FrameSnapshot(1, 1, 10L, intArrayOf(0xff123456.toInt()))
        var received: FrameSnapshot? = null
        val listener: (FrameSnapshot) -> Unit = { received = it }

        FrameSnapshotStore.addListener(listener)
        FrameSnapshotStore.publish(frame)
        FrameSnapshotStore.removeListener(listener)

        assertSame(frame, FrameSnapshotStore.latestFrame)
        assertSame(frame, received)
    }

    @Test
    fun newListenerImmediatelyReceivesLatestFrame() {
        val frame = FrameSnapshot(1, 1, 10L, intArrayOf(0xffabcdef.toInt()))
        FrameSnapshotStore.publish(frame)
        var receivedTimestamp = 0L

        FrameSnapshotStore.addListener { receivedTimestamp = it.timestampMillis }

        assertEquals(10L, receivedTimestamp)
    }
}
