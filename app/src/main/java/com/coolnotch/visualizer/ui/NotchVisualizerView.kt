package com.coolnotch.visualizer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.coolnotch.visualizer.model.ColorMode
import com.coolnotch.visualizer.model.NotchShape
import com.coolnotch.visualizer.model.VisualizerSettings
import com.coolnotch.visualizer.util.ColorExtractor
import kotlin.math.cos
import kotlin.math.sin

class NotchVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var settings: VisualizerSettings = VisualizerSettings()
    private var dynamicColors: Triple<Int, Int, Int>? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var audioMagnitudes: FloatArray = FloatArray(32) { 0.1f }
    private var smoothedMagnitudes: FloatArray = FloatArray(32) { 0.1f }

    private var rotationAngle = 0f
    private val rotationAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 8000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            if (settings.enableRotation) {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
        }
    }

    init {
        rotationAnimator.start()
    }

    fun updateSettings(newSettings: VisualizerSettings) {
        this.settings = newSettings
        invalidate()
    }

    fun updateDynamicColors(primary: Int, secondary: Int, tertiary: Int) {
        this.dynamicColors = Triple(primary, secondary, tertiary)
        invalidate()
    }

    fun updateFft(data: FloatArray) {
        if (data.isEmpty()) return
        val count = settings.barCount.coerceAtLeast(8)
        if (audioMagnitudes.size != count) {
            audioMagnitudes = FloatArray(count)
            smoothedMagnitudes = FloatArray(count)
        }

        val step = (data.size.toFloat() / count).coerceAtLeast(1f)
        val alpha = (1f - settings.smoothing).coerceIn(0.1f, 0.95f)

        for (i in 0 until count) {
            val srcIndex = (i * step).toInt().coerceIn(0, data.size - 1)
            val raw = data[srcIndex] * settings.sensitivity
            smoothedMagnitudes[i] = smoothedMagnitudes[i] * (1f - alpha) + raw * alpha
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = settings.holeRadiusDp * density
        val strokeWidth = settings.visualizerThicknessDp * density

        val colors = resolveColors()

        when (settings.notchShape) {
            NotchShape.CIRCLE_RING -> drawCircleRing(canvas, cx, cy, baseRadius, strokeWidth, colors)
            NotchShape.RADIAL_BARS -> drawRadialBars(canvas, cx, cy, baseRadius, strokeWidth, colors)
            NotchShape.NEON_GLOW -> drawNeonGlow(canvas, cx, cy, baseRadius, strokeWidth, colors)
            NotchShape.WATER_DROP -> drawWaterDrop(canvas, cx, cy, baseRadius, strokeWidth, colors)
            NotchShape.PILL -> drawPillVisualizer(canvas, cx, cy, baseRadius, strokeWidth, colors)
        }
    }

    private fun resolveColors(): Triple<Int, Int, Int> {
        return when (settings.colorMode) {
            ColorMode.DYNAMIC_ALBUM -> {
                dynamicColors ?: Triple(settings.primaryColor, settings.secondaryColor, settings.tertiaryColor)
            }
            ColorMode.GRADIENT_NEON -> {
                Triple(0xFF00F0FF.toInt(), 0xFFFF003C.toInt(), 0xFF7000FF.toInt())
            }
            ColorMode.RAINBOW -> {
                Triple(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
            }
            ColorMode.SINGLE_COLOR -> {
                Triple(settings.primaryColor, settings.primaryColor, settings.primaryColor)
            }
        }
    }

    private fun drawCircleRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        strokeWidth: Float,
        colors: Triple<Int, Int, Int>
    ) {
        val avgEnergy = smoothedMagnitudes.average().toFloat().coerceIn(0.05f, 1.5f)
        val pulseRadius = baseRadius + (avgEnergy * strokeWidth * 2f)

        linePaint.strokeWidth = strokeWidth
        val gradient = SweepGradient(
            cx, cy,
            intArrayOf(colors.first, colors.second, colors.third, colors.first),
            null
        )
        linePaint.shader = gradient

        canvas.save()
        if (settings.enableRotation) {
            canvas.rotate(rotationAngle, cx, cy)
        }
        canvas.drawCircle(cx, cy, pulseRadius, linePaint)
        canvas.restore()
        linePaint.shader = null
    }

    private fun drawRadialBars(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        strokeWidth: Float,
        colors: Triple<Int, Int, Int>
    ) {
        val barCount = settings.barCount.coerceAtLeast(8)
        val angleStep = 360f / barCount
        val barThickness = (strokeWidth * 0.9f).coerceAtLeast(2f)
        linePaint.strokeWidth = barThickness

        canvas.save()
        if (settings.enableRotation) {
            canvas.rotate(rotationAngle * 0.5f, cx, cy)
        }

        for (i in 0 until barCount) {
            val mag = smoothedMagnitudes.getOrElse(i) { 0.2f }.coerceIn(0.05f, 1.5f)
            val barLen = (mag * baseRadius * 0.9f).coerceIn(4f, baseRadius * 1.5f)

            val angleRad = Math.toRadians((i * angleStep).toDouble())
            val startX = cx + (baseRadius + 2f) * cos(angleRad).toFloat()
            val startY = cy + (baseRadius + 2f) * sin(angleRad).toFloat()
            val endX = cx + (baseRadius + 2f + barLen) * cos(angleRad).toFloat()
            val endY = cy + (baseRadius + 2f + barLen) * sin(angleRad).toFloat()

            linePaint.color = when (settings.colorMode) {
                ColorMode.RAINBOW -> ColorExtractor.getRainbowColor(i.toFloat() / barCount)
                else -> if (i % 2 == 0) colors.first else colors.second
            }
            canvas.drawLine(startX, startY, endX, endY, linePaint)
        }
        canvas.restore()
    }

    private fun drawNeonGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        strokeWidth: Float,
        colors: Triple<Int, Int, Int>
    ) {
        val avgEnergy = smoothedMagnitudes.average().toFloat().coerceIn(0.1f, 1.5f)
        val outerRadius = baseRadius + strokeWidth * 3f * avgEnergy

        glowPaint.strokeWidth = strokeWidth * 2f
        glowPaint.color = colors.first
        glowPaint.alpha = (100 * avgEnergy).toInt().coerceIn(30, 200)
        canvas.drawCircle(cx, cy, outerRadius, glowPaint)

        linePaint.strokeWidth = strokeWidth
        linePaint.color = colors.second
        linePaint.alpha = 255
        canvas.drawCircle(cx, cy, baseRadius + strokeWidth, linePaint)
    }

    private fun drawWaterDrop(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        strokeWidth: Float,
        colors: Triple<Int, Int, Int>
    ) {
        val path = Path()
        val numPoints = 36
        val step = 360f / numPoints

        canvas.save()
        if (settings.enableRotation) {
            canvas.rotate(rotationAngle * 0.3f, cx, cy)
        }

        for (i in 0 until numPoints) {
            val angleDeg = i * step
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val magIdx = (i % smoothedMagnitudes.size)
            val mag = smoothedMagnitudes.getOrElse(magIdx) { 0.1f }
            val dynamicR = baseRadius + (mag * strokeWidth * 1.5f) * (1f + 0.3f * sin(angleRad * 2.0).toFloat())

            val px = cx + dynamicR * cos(angleRad).toFloat()
            val py = cy + dynamicR * sin(angleRad).toFloat()

            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        linePaint.strokeWidth = strokeWidth
        linePaint.color = colors.first
        canvas.drawPath(path, linePaint)
        canvas.restore()
    }

    private fun drawPillVisualizer(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        strokeWidth: Float,
        colors: Triple<Int, Int, Int>
    ) {
        val pillWidth = baseRadius * 3.2f
        val pillHeight = baseRadius * 1.8f
        val avgEnergy = smoothedMagnitudes.average().toFloat().coerceIn(0.05f, 1.2f)

        val expandedW = pillWidth + (avgEnergy * strokeWidth * 4f)
        val expandedH = pillHeight + (avgEnergy * strokeWidth * 1.5f)

        val rect = RectF(
            cx - expandedW / 2f,
            cy - expandedH / 2f,
            cx + expandedW / 2f,
            cy + expandedH / 2f
        )

        linePaint.strokeWidth = strokeWidth
        linePaint.color = colors.first
        canvas.drawRoundRect(rect, expandedH / 2f, expandedH / 2f, linePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator.cancel()
    }
}
