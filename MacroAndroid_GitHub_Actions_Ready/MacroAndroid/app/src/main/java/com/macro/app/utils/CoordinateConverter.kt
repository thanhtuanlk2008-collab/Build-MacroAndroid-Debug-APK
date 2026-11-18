package com.macro.app.utils

import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.roundToInt

data class ScreenBounds(
    val widthPx: Int,
    val heightPx: Int,
    val safeLeft: Int = 0,
    val safeTop: Int = 0,
    val safeRight: Int = 0,
    val safeBottom: Int = 0
) {
    val usableWidth: Int get() = (widthPx - safeLeft - safeRight).coerceAtLeast(1)
    val usableHeight: Int get() = (heightPx - safeTop - safeBottom).coerceAtLeast(1)
}

object CoordinateConverter {

    fun getScreenBounds(context: Context, respectSafeArea: Boolean = false): ScreenBounds {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            if (respectSafeArea) {
                val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.displayCutout() or WindowInsets.Type.systemBars()
                )
                ScreenBounds(
                    widthPx = bounds.width(),
                    heightPx = bounds.height(),
                    safeLeft = insets.left,
                    safeTop = insets.top,
                    safeRight = insets.right,
                    safeBottom = insets.bottom
                )
            } else {
                ScreenBounds(widthPx = bounds.width(), heightPx = bounds.height())
            }
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            ScreenBounds(
                widthPx = displayMetrics.widthPixels,
                heightPx = displayMetrics.heightPixels
            )
        }
    }

    /**
     * Converts normalized [0.0, 1.0] coordinates to exact screen pixel coordinates.
     * Clamps output within [0, widthPx - 1] and [0, heightPx - 1] to avoid out-of-bounds Path errors.
     */
    fun toPixel(
        context: Context,
        normX: Float,
        normY: Float,
        respectSafeArea: Boolean = false
    ): PointF {
        val bounds = getScreenBounds(context, respectSafeArea)
        val clampedX = normX.coerceIn(0f, 1f)
        val clampedY = normY.coerceIn(0f, 1f)

        val px = if (respectSafeArea) {
            bounds.safeLeft + clampedX * bounds.usableWidth
        } else {
            clampedX * (bounds.widthPx - 1).coerceAtLeast(1)
        }

        val py = if (respectSafeArea) {
            bounds.safeTop + clampedY * bounds.usableHeight
        } else {
            clampedY * (bounds.heightPx - 1).coerceAtLeast(1)
        }

        return PointF(
            px.coerceIn(0f, (bounds.widthPx - 1).coerceAtLeast(0).toFloat()),
            py.coerceIn(0f, (bounds.heightPx - 1).coerceAtLeast(0).toFloat())
        )
    }

    /**
     * Converts raw screen pixel coordinates into normalized [0.0, 1.0] coordinates.
     */
    fun toNormalized(
        context: Context,
        pixelX: Float,
        pixelY: Float,
        respectSafeArea: Boolean = false
    ): Pair<Float, Float> {
        val bounds = getScreenBounds(context, respectSafeArea)
        val nx = if (respectSafeArea) {
            ((pixelX - bounds.safeLeft) / bounds.usableWidth.toFloat()).coerceIn(0f, 1f)
        } else {
            (pixelX / bounds.widthPx.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
        val ny = if (respectSafeArea) {
            ((pixelY - bounds.safeTop) / bounds.usableHeight.toFloat()).coerceIn(0f, 1f)
        } else {
            (pixelY / bounds.heightPx.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
        return nx to ny
    }

    fun formatPixelPreview(context: Context, normX: Float, normY: Float): String {
        val pt = toPixel(context, normX, normY)
        return "(${pt.x.roundToInt()}, ${pt.y.roundToInt()})"
    }
}
