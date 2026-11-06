package com.coolnotch.visualizer

import com.coolnotch.visualizer.model.ColorMode
import com.coolnotch.visualizer.model.NotchPosition
import com.coolnotch.visualizer.model.NotchShape
import com.coolnotch.visualizer.model.VisualizerSettings
import com.coolnotch.visualizer.util.ColorExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerUnitTests {

    @Test
    fun testDefaultSettings() {
        val settings = VisualizerSettings()
        assertTrue("Visualizer should be enabled by default", settings.isEnabled)
        assertEquals(NotchShape.CIRCLE_RING, settings.notchShape)
        assertEquals(NotchPosition.CENTER, settings.notchPosition)
        assertEquals(ColorMode.DYNAMIC_ALBUM, settings.colorMode)
        assertTrue(settings.holeRadiusDp > 0)
        assertTrue(settings.barCount >= 8)
    }

    @Test
    fun testRainbowColorGeneration() {
        val redColor = ColorExtractor.getRainbowColor(0.0f)
        val greenColor = ColorExtractor.getRainbowColor(0.33f)
        val blueColor = ColorExtractor.getRainbowColor(0.66f)

        assertNotNull(redColor)
        assertNotNull(greenColor)
        assertNotNull(blueColor)
    }

    @Test
    fun testNotchShapesEnumCoverage() {
        val shapes = NotchShape.entries
        assertTrue(shapes.contains(NotchShape.CIRCLE_RING))
        assertTrue(shapes.contains(NotchShape.RADIAL_BARS))
        assertTrue(shapes.contains(NotchShape.NEON_GLOW))
        assertTrue(shapes.contains(NotchShape.WATER_DROP))
        assertTrue(shapes.contains(NotchShape.PILL))
    }

    @Test
    fun testNotchPositionsEnumCoverage() {
        val positions = NotchPosition.entries
        assertTrue(positions.contains(NotchPosition.CENTER))
        assertTrue(positions.contains(NotchPosition.LEFT))
        assertTrue(positions.contains(NotchPosition.RIGHT))
        assertTrue(positions.contains(NotchPosition.CUSTOM))
    }
}
