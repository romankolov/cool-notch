# Cool Notch 🎵✨

An advanced, lightweight Android app that renders a smooth, audio-reactive music visualizer directly around your front-facing selfie camera hole (punch-hole) or display notch.

![Visualizer Concept](app/src/main/res/drawable/ic_tile.xml)

---

## 🌟 Features

- **Quick Settings Tile (`TileService`)**:
  - Toggle the camera visualizer on/off in 1 click straight from your notification bar quick settings!
  - Syncs its active/inactive status immediately across the system.
- **Smart Music Detection (`NotificationListenerService` & `MediaSessionManager`)**:
  - Automatically triggers only when music or media is actively playing (Spotify, YouTube Music, Apple Music, SoundCloud, Tidal, etc.).
  - Stops automatically when paused to conserve 100% battery and CPU.
- **Dynamic Palette from Album Art (`androidx.palette`)**:
  - Automatically extracts vibrant primary, secondary, and accent colors directly from the currently playing album thumbnail art.
  - Supports custom color schemes: **Dynamic Album Art**, **Neon Cyberpunk**, **Rainbow Spectrum**, and **Custom Solid Color**.
- **Versatile Notch & Punch-Hole Geometries**:
  - **Radial Ring**: Pulsing circular aura conforming to punch-hole lenses.
  - **Orbit Bars**: Dynamic radial equalizer frequency bars expanding outwards.
  - **Neon Glow**: Dual-layer breathing neon halo with bloom dissipation.
  - **Water Drop**: Liquid organic deformation synced to low-frequency bass beats.
  - **Pill / Dynamic Island**: Elongated capsule shape for pill-shaped dual camera cutouts or modern pill punch-holes.
- **Multi-Device Notch Support**:
  - Automatic hardware detection via `DisplayCutout` API (`parseCutout`).
  - Presets for **Top Center**, **Top Left**, and **Top Right** cutouts.
  - Interactive sliders to fine-tune radius, thickness, bar count, sensitivity, and smoothing.
- **Lightweight & Battery Efficient**:
  - Uses hardware-accelerated translucent system alert overlays (`FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCHABLE`, `FLAG_LAYOUT_NO_LIMITS`).
  - Seamlessly falls back to dynamic phase simulation if hardware visualizer permissions are restricted by Android OS versions.

---

## 🛠 Project Architecture

```
cool-notch/
├── .github/workflows/
│   └── build-release.yml           # Automated GitHub Actions: tests, builds, signs, and releases APK
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml     # TileService, NotificationListenerService, Foreground Service declarations
│   │   ├── java/com/coolnotch/visualizer/
│   │   │   ├── CoolNotchApp.kt
│   │   │   ├── model/
│   │   │   │   └── VisualizerSettings.kt   # Shapes, positions, palettes, configuration state
│   │   │   ├── service/
│   │   │   │   ├── NotchOverlayService.kt  # System overlay service rendering visualizer
│   │   │   │   ├── VisualizerTileService.kt # Android Quick Settings Tile
│   │   │   │   ├── MediaNotificationListener.kt # Media session & album art listener
│   │   │   │   └── AudioVisualizerEngine.kt # Audio capture & FFT processing
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt         # Customization UI, sliders, live preview & permissions
│   │   │   │   └── NotchVisualizerView.kt  # Custom Canvas view rendering all notch animations
│   │   │   └── util/
│   │   │       ├── NotchDetector.kt        # DisplayCutout API notch detector
│   │   │       ├── ColorExtractor.kt       # Palette extraction from album artwork
│   │   │       └── SettingsManager.kt      # SharedPreferences persistence
│   │   └── res/                            # Layouts, vector icons, themes, and strings
│   ├── src/test/java/                      # Unit tests for settings, models & color generation
│   └── build.gradle.kts
├── gradlew & gradle/                       # Gradle wrapper files
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Building & Testing via GitHub Actions

Every push or pull request to the repository triggers the CI/CD workflow defined in `.github/workflows/build-release.yml`:
1. **Test**: Executes unit test suite (`./gradlew test`).
2. **Compile & Assemble**: Compiles and signs the release APK (`./gradlew assembleRelease`).
3. **Artifact Upload**: Uploads the compiled APK as a workflow artifact available for download.
4. **GitHub Release**: Automatically creates a GitHub release with the final signed `.apk` attached.

### Local Build:
```bash
./gradlew assembleRelease
```
The output APK will be located at: `app/build/outputs/apk/release/app-release.apk`.
