package com.coolnotch.visualizer.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowInsets
import com.coolnotch.visualizer.model.NotchPosition

data class CutoutInfo(
    val centerX: Float,
    val centerY: Float,
    val radiusPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val detectedType: NotchPosition
)

object NotchDetector {

    /**
     * Attempts to automatically detect the camera cutout / notch position and dimensions
     * using DisplayCutout APIs (Android 9+).
     */
    fun detectFromActivity(activity: Activity): CutoutInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        val window = activity.window ?: return null
        val decorView = window.decorView
        val insets = decorView.rootWindowInsets ?: return null
        val cutout = insets.displayCutout ?: return null

        return parseCutout(activity, cutout)
    }

    fun parseCutout(context: Context, cutout: DisplayCutout): CutoutInfo? {
        val boundingRects = cutout.boundingRects
        if (boundingRects.isNullOrEmpty()) return null

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Look for the top cutout (usually camera cutout is on the top edge)
        val topCutout = boundingRects.minByOrNull { it.top } ?: boundingRects.first()

        val cx = topCutout.exactCenterX()
        val cy = topCutout.exactCenterY()
        val width = topCutout.width().toFloat()
        val height = topCutout.height().toFloat()
        val radius = maxOf(width, height) / 2f

        val position = when {
            cx < screenWidth * 0.35f -> NotchPosition.LEFT
            cx > screenWidth * 0.65f -> NotchPosition.RIGHT
            else -> NotchPosition.CENTER
        }

        return CutoutInfo(
            centerX = cx,
            centerY = cy,
            radiusPx = radius,
            widthPx = width,
            heightPx = height,
            detectedType = position
        )
    }

    /**
     * Fallback calculation if DisplayCutout is unavailable or user picks a preset
     */
    fun getPresetCoordinates(
        position: NotchPosition,
        screenWidth: Int,
        holeRadiusPx: Float,
        topMarginPx: Float
    ): Pair<Float, Float> {
        val y = topMarginPx + holeRadiusPx
        val x = when (position) {
            NotchPosition.CENTER -> screenWidth / 2f
            NotchPosition.LEFT -> (screenWidth * 0.12f).coerceAtLeast(holeRadiusPx + 24f)
            NotchPosition.RIGHT -> (screenWidth * 0.88f).coerceAtMost(screenWidth - holeRadiusPx - 24f)
            NotchPosition.CUSTOM -> screenWidth / 2f
        }
        return Pair(x, y)
    }
}
