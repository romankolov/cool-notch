package com.coolnotch.visualizer.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.coolnotch.visualizer.R
import com.coolnotch.visualizer.util.SettingsManager

class VisualizerTileService : TileService() {

    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isNowActive = tile.state != Tile.STATE_ACTIVE
        settingsManager.setEnabled(isNowActive)

        if (isNowActive) {
            val intent = Intent(this, NotchOverlayService::class.java).apply {
                action = NotchOverlayService.ACTION_UPDATE_SETTINGS
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            val intent = Intent(this, NotchOverlayService::class.java).apply {
                action = NotchOverlayService.ACTION_STOP_SERVICE
            }
            startService(intent)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = settingsManager.isEnabled()

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEnabled) {
                getString(R.string.tile_active_subtitle)
            } else {
                getString(R.string.tile_inactive_subtitle)
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }
}
