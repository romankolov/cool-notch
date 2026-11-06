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
        val hsv = floatArrayOf((fraction * 360f) % 360f, 0.9f, 1.0f)
        return android.graphics.Color.HSVToColor(hsv)
    }
}
