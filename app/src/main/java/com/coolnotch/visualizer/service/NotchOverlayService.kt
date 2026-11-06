package com.coolnotch.visualizer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.coolnotch.visualizer.R
import com.coolnotch.visualizer.model.NotchPosition
import com.coolnotch.visualizer.model.VisualizerSettings
import com.coolnotch.visualizer.ui.MainActivity
import com.coolnotch.visualizer.ui.NotchVisualizerView
import com.coolnotch.visualizer.util.NotchDetector
import com.coolnotch.visualizer.util.SettingsManager

class NotchOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var settingsManager: SettingsManager
    private var visualizerView: NotchVisualizerView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var audioEngine: AudioVisualizerEngine
    private var isOverlayAttached = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)

        startForegroundServiceNotification()

        audioEngine = AudioVisualizerEngine { fftData ->
            visualizerView?.post {
                visualizerView?.updateFft(fftData)
            }
        }

        val settings = settingsManager.loadSettings()
        if (settings.isEnabled) {
            showOverlay(settings)
            audioEngine.startCapture(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_SETTINGS -> {
                val settings = settingsManager.loadSettings()
                if (settings.isEnabled) {
                    showOverlay(settings)
                    visualizerView?.updateSettings(settings)
                    audioEngine.startCapture(this)
                } else {
                    hideOverlay()
                    audioEngine.stopCapture()
                }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            ACTION_UPDATE_COLORS -> {
                val p = intent.getIntExtra(EXTRA_PRIMARY, 0)
                val s = intent.getIntExtra(EXTRA_SECONDARY, 0)
                val t = intent.getIntExtra(EXTRA_TERTIARY, 0)
                visualizerView?.updateDynamicColors(p, s, t)
            }
            ACTION_PLAYBACK_STATE_CHANGED -> {
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val settings = settingsManager.loadSettings()
                if (settings.showOnlyWhenPlaying) {
                    if (isPlaying && settings.isEnabled) {
                        showOverlay(settings)
                        audioEngine.startCapture(this)
                    } else {
                        hideOverlay()
                        audioEngine.stopCapture()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun showOverlay(settings: VisualizerSettings) {
        if (isOverlayAttached && visualizerView != null) {
            updateOverlayPosition(settings)
            return
        }

        try {
            val view = NotchVisualizerView(this)
            view.updateSettings(settings)

            val displayMetrics = resources.displayMetrics
            val density = displayMetrics.density
            val diameterDp = (settings.holeRadiusDp + settings.visualizerThicknessDp * 4f) * 2f
            val viewSizePx = (diameterDp * density).toInt().coerceAtLeast((72 * density).toInt())

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val coords = calculateCoordinates(settings, displayMetrics, viewSizePx)

            val params = WindowManager.LayoutParams(
                viewSizePx,
                viewSizePx,
                coords.first - viewSizePx / 2,
                coords.second - viewSizePx / 2,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager.addView(view, params)
            this.visualizerView = view
            this.layoutParams = params
            this.isOverlayAttached = true
            Log.d(TAG, "Notch overlay view successfully attached to window")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add visualizer overlay to WindowManager: ${e.message}")
        }
    }

    private fun updateOverlayPosition(settings: VisualizerSettings) {
        val view = visualizerView ?: return
        val params = layoutParams ?: return
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val diameterDp = (settings.holeRadiusDp + settings.visualizerThicknessDp * 4f) * 2f
        val viewSizePx = (diameterDp * density).toInt().coerceAtLeast((72 * density).toInt())

        params.width = viewSizePx
        params.height = viewSizePx

        val coords = calculateCoordinates(settings, displayMetrics, viewSizePx)
        params.x = coords.first - viewSizePx / 2
        params.y = coords.second - viewSizePx / 2

        try {
            windowManager.updateViewLayout(view, params)
            view.updateSettings(settings)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update view layout: ${e.message}")
        }
    }

    private fun calculateCoordinates(
        settings: VisualizerSettings,
        displayMetrics: DisplayMetrics,
        viewSizePx: Int
    ): Pair<Int, Int> {
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density
        val holeRadiusPx = settings.holeRadiusDp * density
        val topMarginPx = 16f * density

        return if (settings.notchPosition == NotchPosition.CUSTOM && settings.customX >= 0 && settings.customY >= 0) {
            Pair(settings.customX.toInt(), settings.customY.toInt())
        } else {
            val (x, y) = NotchDetector.getPresetCoordinates(
                settings.notchPosition,
                screenWidth,
                holeRadiusPx,
                topMarginPx
            )
            Pair(x.toInt(), y.toInt())
        }
    }

    private fun hideOverlay() {
        if (isOverlayAttached && visualizerView != null) {
            try {
                windowManager.removeView(visualizerView)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing overlay view: ${e.message}")
            }
            visualizerView = null
            isOverlayAttached = false
        }
    }

    private fun startForegroundServiceNotification() {
        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_active_desc))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.stopCapture()
        hideOverlay()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "NotchOverlayService"
        const val CHANNEL_ID = "cool_notch_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_UPDATE_SETTINGS = "com.coolnotch.visualizer.UPDATE_SETTINGS"
        const val ACTION_STOP_SERVICE = "com.coolnotch.visualizer.STOP_SERVICE"
        const val ACTION_UPDATE_COLORS = "com.coolnotch.visualizer.UPDATE_COLORS"
        const val ACTION_PLAYBACK_STATE_CHANGED = "com.coolnotch.visualizer.PLAYBACK_CHANGED"

        const val EXTRA_PRIMARY = "extra_primary"
        const val EXTRA_SECONDARY = "extra_secondary"
        const val EXTRA_TERTIARY = "extra_tertiary"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var instance: NotchOverlayService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
