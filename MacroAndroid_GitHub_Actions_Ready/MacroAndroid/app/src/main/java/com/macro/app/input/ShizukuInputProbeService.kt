package com.macro.app.input

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

/**
 * Runs inside Shizuku's UserService process.
 *
 * This phase is diagnostic only: it verifies the actual process UID and
 * whether that process has INJECT_EVENTS. It deliberately does not inject
 * any MotionEvent until the target device's permission and concurrency
 * behavior have been separately validated.
 */
class ShizukuInputProbeService(
    private val serviceContext: Context
) : IShizukuInputProbe.Stub() {

    override fun getProcessUid(): Int = Process.myUid()

    override fun getProcessPid(): Int = Process.myPid()

    override fun hasInjectEventsPermission(): Boolean =
        serviceContext.checkPermission(
            Manifest.permission.INJECT_EVENTS,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
}
