package com.macro.app.input

import android.content.Context
import android.content.pm.PackageManager

/**
 * Reports what this app process can prove about input injection.
 *
 * Checking INJECT_EVENTS here checks the app UID only. It does NOT inspect a
 * Shizuku UserService running as shell (UID 2000), and it does not prove that
 * shell can inject touchscreen events on a particular OEM build.
 */
data class InputBackendAssessment(
    val appUidHasInjectEvents: Boolean,
    val accessibilityConnected: Boolean,
    val independentTouchStreamAvailable: Boolean,
    val selectedFallback: String?,
    val message: String
)

object InputBackendSelector {
    private const val PERMISSION_INJECT_EVENTS = "android.permission.INJECT_EVENTS"

    fun assess(
        context: Context,
        accessibilityConnected: Boolean
    ): InputBackendAssessment {
        val appUidHasInjectEvents =
            context.checkSelfPermission(PERMISSION_INJECT_EVENTS) ==
                PackageManager.PERMISSION_GRANTED

        // No independent injector is implemented or validated yet. Even if
        // this app UID has a privileged permission, a backend must still be
        // implemented and concurrency-tested before enabling this flag.
        val fallback = if (accessibilityConnected) "accessibility-dispatchGesture" else null

        val message = when {
            !accessibilityConnected ->
                "Chưa có backend inject khả dụng. Accessibility Service chưa kết nối."
            appUidHasInjectEvents ->
                "App UID có INJECT_EVENTS, nhưng chưa có backend độc lập đã được kiểm chứng."
            else ->
                "Chỉ có Accessibility fallback. dispatchGesture có thể xung đột với thao tác chạm đang diễn ra."
        }

        return InputBackendAssessment(
            appUidHasInjectEvents = appUidHasInjectEvents,
            accessibilityConnected = accessibilityConnected,
            independentTouchStreamAvailable = false,
            selectedFallback = fallback,
            message = message
        )
    }
}
