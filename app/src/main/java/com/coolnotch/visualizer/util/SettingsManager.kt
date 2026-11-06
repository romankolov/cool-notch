package com.coolnotch.visualizer.util

import android.content.Context
import android.content.SharedPreferences
import com.coolnotch.visualizer.model.ColorMode
import com.coolnotch.visualizer.model.NotchPosition
import com.coolnotch.visualizer.model.NotchShape
import com.coolnotch.visualizer.model.VisualizerSettings

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): VisualizerSettings {
        return VisualizerSettings(
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, true),
            notchShape = NotchShape.entries.getOrNull(
                prefs.getInt(KEY_SHAPE, NotchShape.CIRCLE_RING.ordinal)
            ) ?: NotchShape.CIRCLE_RING,
            notchPosition = NotchPosition.entries.getOrNull(
                prefs.getInt(KEY_POSITION, NotchPosition.CENTER.ordinal)
            ) ?: NotchPosition.CENTER,
            customX = prefs.getFloat(KEY_CUSTOM_X, -1f),
            customY = prefs.getFloat(KEY_CUSTOM_Y, -1f),
            holeRadiusDp = prefs.getFloat(KEY_RADIUS_DP, 16f),
            visualizerThicknessDp = prefs.getFloat(KEY_THICKNESS_DP, 5f),
            barCount = prefs.getInt(KEY_BAR_COUNT, 24),
            colorMode = ColorMode.entries.getOrNull(
                prefs.getInt(KEY_COLOR_MODE, ColorMode.DYNAMIC_ALBUM.ordinal)
            ) ?: ColorMode.DYNAMIC_ALBUM,
            primaryColor = prefs.getInt(KEY_PRIMARY_COLOR, 0xFF00E5FF.toInt()),
            secondaryColor = prefs.getInt(KEY_SECONDARY_COLOR, 0xFFFF007F.toInt()),
            tertiaryColor = prefs.getInt(KEY_TERTIARY_COLOR, 0xFF7C4DFF.toInt()),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.0f),
            smoothing = prefs.getFloat(KEY_SMOOTHING, 0.5f),
            showOnlyWhenPlaying = prefs.getBoolean(KEY_SHOW_ONLY_PLAYING, true),
            enableRotation = prefs.getBoolean(KEY_ENABLE_ROTATION, true)
        )
    }

    fun saveSettings(settings: VisualizerSettings) {
        prefs.edit()
            .putBoolean(KEY_IS_ENABLED, settings.isEnabled)
            .putInt(KEY_SHAPE, settings.notchShape.ordinal)
            .putInt(KEY_POSITION, settings.notchPosition.ordinal)
            .putFloat(KEY_CUSTOM_X, settings.customX)
            .putFloat(KEY_CUSTOM_Y, settings.customY)
            .putFloat(KEY_RADIUS_DP, settings.holeRadiusDp)
            .putFloat(KEY_THICKNESS_DP, settings.visualizerThicknessDp)
            .putInt(KEY_BAR_COUNT, settings.barCount)
            .putInt(KEY_COLOR_MODE, settings.colorMode.ordinal)
            .putInt(KEY_PRIMARY_COLOR, settings.primaryColor)
            .putInt(KEY_SECONDARY_COLOR, settings.secondaryColor)
            .putInt(KEY_TERTIARY_COLOR, settings.tertiaryColor)
            .putFloat(KEY_SENSITIVITY, settings.sensitivity)
            .putFloat(KEY_SMOOTHING, settings.smoothing)
            .putBoolean(KEY_SHOW_ONLY_PLAYING, settings.showOnlyWhenPlaying)
            .putBoolean(KEY_ENABLE_ROTATION, settings.enableRotation)
            .apply()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ENABLED, enabled).apply()
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_IS_ENABLED, true)

    companion object {
        private const val PREFS_NAME = "cool_notch_prefs"
        private const val KEY_IS_ENABLED = "key_is_enabled"
        private const val KEY_SHAPE = "key_shape"
        private const val KEY_POSITION = "key_position"
        private const val KEY_CUSTOM_X = "key_custom_x"
        private const val KEY_CUSTOM_Y = "key_custom_y"
        private const val KEY_RADIUS_DP = "key_radius_dp"
        private const val KEY_THICKNESS_DP = "key_thickness_dp"
        private const val KEY_BAR_COUNT = "key_bar_count"
        private const val KEY_COLOR_MODE = "key_color_mode"
        private const val KEY_PRIMARY_COLOR = "key_primary_color"
        private const val KEY_SECONDARY_COLOR = "key_secondary_color"
        private const val KEY_TERTIARY_COLOR = "key_tertiary_color"
        private const val KEY_SENSITIVITY = "key_sensitivity"
        private const val KEY_SMOOTHING = "key_smoothing"
        private const val KEY_SHOW_ONLY_PLAYING = "key_show_only_playing"
        private const val KEY_ENABLE_ROTATION = "key_enable_rotation"
    }
}
