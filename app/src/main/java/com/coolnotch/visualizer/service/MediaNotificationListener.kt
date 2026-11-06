package com.coolnotch.visualizer.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.coolnotch.visualizer.util.ColorExtractor
import com.coolnotch.visualizer.util.SettingsManager

class MediaNotificationListener : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var settingsManager: SettingsManager

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveMediaControllers(controllers)
    }

    private val activeCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
        try {
            val component = ComponentName(this, MediaNotificationListener::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, component)
            val controllers = mediaSessionManager.getActiveSessions(component)
            updateActiveMediaControllers(controllers)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing active session access permission: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            clearCallbacks()
        } catch (e: Exception) {
            Log.w(TAG, "Error removing listener: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val extras = sbn.notification.extras ?: return
        val isMedia = sbn.notification.category == Notification.CATEGORY_TRANSPORT ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

        if (isMedia) {
            // Attempt to pull large icon (album artwork)
            @Suppress("DEPRECATION")
            val largeIconBitmap = (extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
                ?: (extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap)
                ?: extractBitmapFromIcon(sbn.notification.getLargeIcon())

            if (largeIconBitmap != null) {
                dispatchArtworkColors(largeIconBitmap)
            }
        }
    }

    private fun extractBitmapFromIcon(icon: Icon?): Bitmap? {
        if (icon == null) return null
        return try {
            val drawable = icon.loadDrawable(this)
            (drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun updateActiveMediaControllers(controllers: List<MediaController>?) {
        clearCallbacks()
        if (controllers.isNullOrEmpty()) {
            notifyPlaybackState(false)
            return
        }

        var anyPlaying = false
        for (controller in controllers) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                anyPlaying = true
                controller.metadata?.let { extractColorsFromMetadata(it) }
            }

            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                    notifyPlaybackState(isPlaying)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    metadata?.let { extractColorsFromMetadata(it) }
                }
            }
            controller.registerCallback(cb)
            activeCallbacks[controller] = cb
        }

        notifyPlaybackState(anyPlaying)
    }

    private fun extractColorsFromMetadata(metadata: MediaMetadata) {
        val artBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        if (artBitmap != null) {
            dispatchArtworkColors(artBitmap)
        }
    }

    private fun dispatchArtworkColors(bitmap: Bitmap) {
        val colors = ColorExtractor.extractFromBitmap(bitmap)
        val intent = Intent(this, NotchOverlayService::class.java).apply {
            action = NotchOverlayService.ACTION_UPDATE_COLORS
            putExtra(NotchOverlayService.EXTRA_PRIMARY, colors.primary)
            putExtra(NotchOverlayService.EXTRA_SECONDARY, colors.secondary)
            putExtra(NotchOverlayService.EXTRA_TERTIARY, colors.tertiary)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot forward colors to overlay service: ${e.message}")
        }
    }

    private fun notifyPlaybackState(isPlaying: Boolean) {
        val intent = Intent(this, NotchOverlayService::class.java).apply {
            action = NotchOverlayService.ACTION_PLAYBACK_STATE_CHANGED
            putExtra(NotchOverlayService.EXTRA_IS_PLAYING, isPlaying)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot notify playback state: ${e.message}")
        }
    }

    private fun clearCallbacks() {
        for ((controller, cb) in activeCallbacks) {
            try {
                controller.unregisterCallback(cb)
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeCallbacks.clear()
    }

    companion object {
        private const val TAG = "MediaNotifListener"
    }
}
