package com.soulknight.matrixai.macro

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Produces timed macro events only. Touch injection belongs to a separately
 * permissioned InputController and is intentionally not implemented here.
 */
class MacroScheduler(
    private val onAction: (MacroAction) -> Unit,
    private val onError: (MacroAction, Exception) -> Unit = { _, _ -> }
) {
    private var executor: ScheduledExecutorService? = null
    private val running = AtomicBoolean(false)

    /**
     * Starts one recurring task per enabled action.
     * @return false when the configuration has no enabled actions.
     */
    @Synchronized
    fun start(config: MacroConfig): Boolean {
        stop()

        val safe = config.validated()
        val enabledActions = MacroAction.values().filter(safe::isEnabled)
        if (enabledActions.isEmpty()) return false

        val worker = Executors.newScheduledThreadPool(enabledActions.size) { runnable ->
            Thread(runnable, "MatrixMacroScheduler").apply { isDaemon = true }
        }
        executor = worker
        running.set(true)

        enabledActions.forEach { action ->
            worker.scheduleAtFixedRate({
                if (running.get()) {
                    try {
                        onAction(action)
                    } catch (failure: Exception) {
                        // A UI/logging callback failure must not cancel future ticks.
                        try {
                            onError(action, failure)
                        } catch (_: Exception) {
                            // Keep the scheduler alive even if error reporting fails.
                        }
                    }
                }
            }, 0L, safe.intervalFor(action), TimeUnit.MILLISECONDS)
        }
        return true
    }

    @Synchronized
    fun stop() {
        running.set(false)
        executor?.shutdownNow()
        executor = null
    }

    fun isRunning(): Boolean = running.get()
}
