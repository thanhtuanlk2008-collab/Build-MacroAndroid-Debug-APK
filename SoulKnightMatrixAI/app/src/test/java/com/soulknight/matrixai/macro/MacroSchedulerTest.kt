package com.soulknight.matrixai.macro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MacroSchedulerTest {
    @Test
    fun startEmitsEnabledActionAndStopDisablesScheduler() {
        val latch = CountDownLatch(1)
        val scheduler = MacroScheduler(onAction = { action ->
            if (action == MacroAction.ATTACK) latch.countDown()
        })

        try {
            val started = scheduler.start(
                MacroConfig(
                    attackEnabled = true,
                    attackIntervalMs = 100L,
                    skillEnabled = false,
                    switchWeaponEnabled = false
                )
            )

            assertTrue(started)
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertTrue(scheduler.isRunning())
        } finally {
            scheduler.stop()
        }

        assertFalse(scheduler.isRunning())
    }

    @Test
    fun startReturnsFalseWhenNoActionIsEnabled() {
        val calls = AtomicInteger(0)
        val scheduler = MacroScheduler(onAction = { calls.incrementAndGet() })

        val started = scheduler.start(
            MacroConfig(
                attackEnabled = false,
                skillEnabled = false,
                switchWeaponEnabled = false
            )
        )

        assertFalse(started)
        assertFalse(scheduler.isRunning())
        scheduler.stop()
        assertTrue(calls.get() == 0)
    }

    @Test
    fun callbackExceptionDoesNotCancelFutureTicks() {
        val actionCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val secondTick = CountDownLatch(2)
        val scheduler = MacroScheduler(
            onAction = {
                actionCount.incrementAndGet()
                secondTick.countDown()
                if (actionCount.get() == 1) error("test callback failure")
            },
            onError = { _, _ -> errorCount.incrementAndGet() }
        )

        try {
            assertTrue(
                scheduler.start(
                    MacroConfig(
                        attackEnabled = true,
                        attackIntervalMs = 100L,
                        skillEnabled = false,
                        switchWeaponEnabled = false
                    )
                )
            )
            assertTrue(secondTick.await(2, TimeUnit.SECONDS))
            assertTrue(errorCount.get() >= 1)
            assertTrue(actionCount.get() >= 2)
        } finally {
            scheduler.stop()
        }
    }
}
