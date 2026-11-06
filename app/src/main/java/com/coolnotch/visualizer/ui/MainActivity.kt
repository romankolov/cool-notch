package com.coolnotch.visualizer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.coolnotch.visualizer.R
import com.coolnotch.visualizer.databinding.ActivityMainBinding
import com.coolnotch.visualizer.model.ColorMode
import com.coolnotch.visualizer.model.NotchPosition
import com.coolnotch.visualizer.model.NotchShape
import com.coolnotch.visualizer.model.VisualizerSettings
import com.coolnotch.visualizer.service.NotchOverlayService
import com.coolnotch.visualizer.util.NotchDetector
import com.coolnotch.visualizer.util.SettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var currentSettings: VisualizerSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        currentSettings = settingsManager.loadSettings()

        setupSpinners()
        setupSliders()
        setupSwitches()
        setupPermissionButtons()
        setupAutoDetect()
        updatePreview()

        if (currentSettings.isEnabled && hasOverlayPermission()) {
            startOverlayService()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButtonStates()
    }

    private fun setupSpinners() {
        // Shapes
        val shapeNames = NotchShape.entries.map { it.displayName }
        val shapeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shapeNames)
        binding.spinnerShape.adapter = shapeAdapter
        binding.spinnerShape.setSelection(currentSettings.notchShape.ordinal)
        binding.spinnerShape.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                currentSettings.notchShape = NotchShape.entries[position]
                saveAndApply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Positions
        val positionNames = NotchPosition.entries.map { it.displayName }
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, positionNames)
        binding.spinnerPosition.adapter = posAdapter
        binding.spinnerPosition.setSelection(currentSettings.notchPosition.ordinal)
        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                currentSettings.notchPosition = NotchPosition.entries[position]
                saveAndApply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Color Modes
        val colorNames = ColorMode.entries.map { it.displayName }
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colorNames)
        binding.spinnerColorMode.adapter = colorAdapter
        binding.spinnerColorMode.setSelection(currentSettings.colorMode.ordinal)
        binding.spinnerColorMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                currentSettings.colorMode = ColorMode.entries[position]
                saveAndApply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupSliders() {
        binding.sliderRadius.value = currentSettings.holeRadiusDp
        binding.tvRadiusLabel.text = "Hole Radius (dp): ${currentSettings.holeRadiusDp.toInt()}"
        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            currentSettings.holeRadiusDp = value
            binding.tvRadiusLabel.text = "Hole Radius (dp): ${value.toInt()}"
            saveAndApply()
        }

        binding.sliderThickness.value = currentSettings.visualizerThicknessDp
        binding.tvThicknessLabel.text = "Ring Thickness (dp): ${currentSettings.visualizerThicknessDp.toInt()}"
        binding.sliderThickness.addOnChangeListener { _, value, _ ->
            currentSettings.visualizerThicknessDp = value
            binding.tvThicknessLabel.text = "Ring Thickness (dp): ${value.toInt()}"
            saveAndApply()
        }

        binding.sliderBars.value = currentSettings.barCount.toFloat()
        binding.tvBarsLabel.text = "Bar Count: ${currentSettings.barCount}"
        binding.sliderBars.addOnChangeListener { _, value, _ ->
            currentSettings.barCount = value.toInt()
            binding.tvBarsLabel.text = "Bar Count: ${value.toInt()}"
            saveAndApply()
        }
    }

    private fun setupSwitches() {
        binding.switchEnable.isChecked = currentSettings.isEnabled
        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            currentSettings.isEnabled = isChecked
            saveAndApply()
            if (isChecked) {
                if (hasOverlayPermission()) {
                    startOverlayService()
                } else {
                    requestOverlayPermission()
                }
            } else {
                stopOverlayService()
            }
        }

        binding.switchOnlyPlaying.isChecked = currentSettings.showOnlyWhenPlaying
        binding.switchOnlyPlaying.setOnCheckedChangeListener { _, isChecked ->
            currentSettings.showOnlyWhenPlaying = isChecked
            saveAndApply()
        }

        binding.switchRotate.isChecked = currentSettings.enableRotation
        binding.switchRotate.setOnCheckedChangeListener { _, isChecked ->
            currentSettings.enableRotation = isChecked
            saveAndApply()
        }
    }

    private fun setupPermissionButtons() {
        binding.btnOverlayPerm.setOnClickListener {
            requestOverlayPermission()
        }
        binding.btnAudioPerm.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
        binding.btnNotifPerm.setOnClickListener {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    private fun setupAutoDetect() {
        binding.btnAutoDetectCutout.setOnClickListener {
            val cutoutInfo = NotchDetector.detectFromActivity(this)
            if (cutoutInfo != null) {
                val density = resources.displayMetrics.density
                currentSettings.customX = cutoutInfo.centerX
                currentSettings.customY = cutoutInfo.centerY
                currentSettings.holeRadiusDp = (cutoutInfo.radiusPx / density).coerceIn(8f, 36f)
                currentSettings.notchPosition = cutoutInfo.detectedType

                binding.spinnerPosition.setSelection(cutoutInfo.detectedType.ordinal)
                binding.sliderRadius.value = currentSettings.holeRadiusDp
                saveAndApply()

                Toast.makeText(
                    this,
                    "${getString(R.string.detected_cutout_msg)} ${cutoutInfo.detectedType.displayName} (r=${currentSettings.holeRadiusDp.toInt()}dp)",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, getString(R.string.no_cutout_detected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePermissionButtonStates() {
        val hasOverlay = hasOverlayPermission()
        binding.btnOverlayPerm.isEnabled = !hasOverlay
        binding.btnOverlayPerm.text = if (hasOverlay) "Overlay: Granted ✓" else getString(R.string.perm_overlay)

        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        binding.btnAudioPerm.isEnabled = !hasAudio
        binding.btnAudioPerm.text = if (hasAudio) "Audio FFT: Granted ✓" else getString(R.string.perm_audio)

        val notifGranted = isNotificationServiceEnabled()
        binding.btnNotifPerm.isEnabled = !notifGranted
        binding.btnNotifPerm.text = if (notifGranted) "Media Access: Granted ✓" else getString(R.string.perm_notification)
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun saveAndApply() {
        settingsManager.saveSettings(currentSettings)
        updatePreview()

        if (currentSettings.isEnabled && hasOverlayPermission()) {
            val intent = Intent(this, NotchOverlayService::class.java).apply {
                action = NotchOverlayService.ACTION_UPDATE_SETTINGS
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun updatePreview() {
        binding.previewVisualizerView.updateSettings(currentSettings)
        // Feed sample dynamic test values to preview visualizer view
        val sampleFft = FloatArray(currentSettings.barCount) { idx ->
            0.3f + 0.5f * kotlin.math.abs(kotlin.math.sin(idx.toDouble() * 0.5)).toFloat()
        }
        binding.previewVisualizerView.updateFft(sampleFft)
    }

    private fun startOverlayService() {
        val intent = Intent(this, NotchOverlayService::class.java).apply {
            action = NotchOverlayService.ACTION_UPDATE_SETTINGS
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, NotchOverlayService::class.java).apply {
            action = NotchOverlayService.ACTION_STOP_SERVICE
        }
        startService(intent)
    }

    companion object {
        private const val REQ_AUDIO = 101
    }
}
