package com.coolnotch.visualizer.util

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

data class ExtractedColors(
    val primary: Int,
    val secondary: Int,
    val tertiary: Int
)

object ColorExtractor {

    // Default neon fallback palette
    private const val DEFAULT_PRIMARY = 0xFF00E5FF.toInt()   // Vibrant Cyan
    private const val DEFAULT_SECONDARY = 0xFFFF007F.toInt() // Vibrant Pink / Magenta
    private const val DEFAULT_TERTIARY = 0xFF7C4DFF.toInt()  // Deep Purple

    /**
     * Extracts harmonious visualizer colors from a music album artwork bitmap.
     */
    fun extractFromBitmap(bitmap: Bitmap): ExtractedColors {
        val palette = try {
            Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()
        } catch (e: Exception) {
            return ExtractedColors(DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_TERTIARY)
        }

        val vibrant = palette.vibrantSwatch?.rgb
        val lightVibrant = palette.lightVibrantSwatch?.rgb
        val darkVibrant = palette.darkVibrantSwatch?.rgb
        val dominant = palette.dominantSwatch?.rgb
        val muted = palette.mutedSwatch?.rgb

        val primary = vibrant ?: dominant ?: DEFAULT_PRIMARY
        val secondary = lightVibrant ?: darkVibrant ?: muted ?: DEFAULT_SECONDARY
        val tertiary = darkVibrant ?: muted ?: dominant ?: DEFAULT_TERTIARY

        return ExtractedColors(primary, secondary, tertiary)
    }

    /**
     * Generate rainbow colors interpolated along circle degrees.
     */
    fun getRainbowColor(fraction: Float): Int {
        val h = ((fraction * 360f) % 360f + 360f) % 360f
        val s = 0.9f
        val v = 1.0f

        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255).toInt()
        val g = ((gPrime + m) * 255).toInt()
        val b = ((bPrime + m) * 255).toInt()

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
