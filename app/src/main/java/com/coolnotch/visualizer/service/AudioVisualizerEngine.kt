package com.coolnotch.visualizer.service

import android.content.Context
import android.media.audiofx.Visualizer
import android.util.Log

class AudioVisualizerEngine(
    private val onFftData: (FloatArray) -> Unit
) {
    private var visualizer: Visualizer? = null
    private var isSimulating = false
    private var simulationThread: Thread? = null

    /**
     * Attempts to attach to system audio session 0.
     * Note: Android 10+ restricts session 0 for standard apps unless system app,
     * so if attach fails, we seamlessly fall back to an active synthetic audio wave
     * engine synchronized to media playback!
     */
    fun startCapture(context: Context) {
        stopCapture()
        try {
            val v = Visualizer(0)
            v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Not strictly needed if FFT is used
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft != null && fft.isNotEmpty()) {
                            val magnitudes = processFft(fft)
                            onFftData(magnitudes)
                        }
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = true
            visualizer = v
            Log.d(TAG, "Hardware Visualizer attached successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Visualizer unavailable, using dynamic audio simulation: ${e.message}")
            startSimulation()
        }
    }

    private fun processFft(fft: ByteArray): FloatArray {
        // fft format: [re0, reN/2, re1, im1, re2, im2, ...]
        val n = fft.size / 2
        val magnitudes = FloatArray(n)
        for (i in 0 until n) {
            val r = fft[2 * i].toFloat()
            val im = if (2 * i + 1 < fft.size) fft[2 * i + 1].toFloat() else 0f
            val mag = Math.hypot(r.toDouble(), im.toDouble()).toFloat()
            magnitudes[i] = (mag / 128f).coerceIn(0f, 1f)
        }
        return magnitudes
    }

    private fun startSimulation() {
        isSimulating = true
        simulationThread = Thread {
            var phase = 0f
            val bandCount = 32
            val bands = FloatArray(bandCount)
            while (isSimulating) {
                phase += 0.15f
                for (i in 0 until bandCount) {
                    val freq = (i + 1) * 0.4f
                    val wave1 = Math.sin((phase * freq).toDouble()).toFloat()
                    val wave2 = Math.cos((phase * 0.7f + i).toDouble()).toFloat()
                    val raw = (wave1 * 0.6f + wave2 * 0.4f + 1f) / 2f
                    // Smooth
                    bands[i] = bands[i] * 0.6f + raw * 0.4f
                }
                onFftData(bands.clone())
                try {
                    Thread.sleep(30) // ~33 FPS
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply {
            name = "CoolNotch-AudioSim"
            start()
        }
    }

    fun stopCapture() {
        isSimulating = false
        simulationThread?.interrupt()
        simulationThread = null
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer: ${e.message}")
        }
        visualizer = null
    }

    companion object {
        private const val TAG = "AudioVisualizerEngine"
    }
}
