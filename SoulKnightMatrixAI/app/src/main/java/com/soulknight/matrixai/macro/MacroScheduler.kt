package com.soulknight.matrixai.macro

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Produces timed macro events. It deliberately does not inject touch input:
 * a separately permissioned and device-tested InputController is required.
 */
class MacroScheduler(
    private val onAction: (MacroAction) -> Unit
) {
    private var executor: ScheduledExecutorService? = null
    private val running = AtomicBoolean(false)

    @Synchronized
    fun start(config: MacroConfig) {
        stop()
        val safe = config.validated()
        val worker = Executors.newScheduledThreadPool(3) { runnable ->
            Thread(runnable, "MatrixMacroScheduler").apply { isDaemon = true }
        }
        executor = worker
        running.set(true)

        MacroAction.values().forEach { action ->
            if (safe.isEnabled(action)) {
                val interval = safe.intervalFor(action)
                worker.scheduleAtFixedRate({
                    if (running.get()) onAction(action)
                }, 0L, interval, TimeUnit.MILLISECONDS)
            }
        }
    }

    @Synchronized
    fun stop() {
        running.set(false)
        executor?.shutdownNow()
        executor = null
    }

    fun isRunning(): Boolean = running.get()
}
