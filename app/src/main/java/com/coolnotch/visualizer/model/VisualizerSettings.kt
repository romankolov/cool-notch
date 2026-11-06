package com.coolnotch.visualizer.model

enum class NotchShape(val displayName: String) {
    CIRCLE_RING("Radial Ring / Punch Hole"),
    RADIAL_BARS("Pulsing Orbit Bars"),
    NEON_GLOW("Neon Halo Glow"),
    WATER_DROP("Water Drop / Teardrop"),
    PILL("Pill / Dynamic Island")
}

enum class NotchPosition(val displayName: String) {
    CENTER("Top Center"),
    LEFT("Top Left"),
    RIGHT("Top Right"),
    CUSTOM("Custom Coordinates")
}

enum class ColorMode(val displayName: String) {
    DYNAMIC_ALBUM("Auto from Album Art"),
    GRADIENT_NEON("Neon Cyberpunk"),
    RAINBOW("Rainbow Spectrum"),
    SINGLE_COLOR("Custom Solid Color")
}

data class VisualizerSettings(
    var isEnabled: Boolean = true,
    var notchShape: NotchShape = NotchShape.CIRCLE_RING,
    var notchPosition: NotchPosition = NotchPosition.CENTER,
    var customX: Float = -1f,
    var customY: Float = -1f,
    var holeRadiusDp: Float = 16f,
    var visualizerThicknessDp: Float = 5f,
    var barCount: Int = 24,
    var colorMode: ColorMode = ColorMode.DYNAMIC_ALBUM,
    var primaryColor: Int = 0xFF00E5FF.toInt(), // Cyan
    var secondaryColor: Int = 0xFFFF007F.toInt(), // Magenta
    var tertiaryColor: Int = 0xFF7C4DFF.toInt(), // Purple
    var sensitivity: Float = 1.0f,
    var smoothing: Float = 0.5f,
    var showOnlyWhenPlaying: Boolean = true,
    var enableRotation: Boolean = true
)
