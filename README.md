<p align="center">
  <img src="docs/assets/nexplay-logo.png" width="180" alt="NexPlay logo">
</p>

<h1 align="center">NexPlay</h1>

<p align="center">
  <strong>Play Everything.</strong><br>
  A local-first native Android media player for your own music and videos.
</p>

<p align="center">
  Android 6.0+ · Kotlin · Jetpack Compose · Media3
</p>

---

## Overview

NexPlay is a native Android media player designed around local media ownership.

It discovers supported audio and video available to the device, keeps playback state local, and provides a focused interface for browsing folders, searching media, managing playlists, and controlling playback.

NexPlay does not require an account or cloud media library for its core playback experience.

## Highlights

- Local-first audio and video playback
- Folder-based media browsing
- Fast media search
- Native Audio Player
- Native Video Player
- Background audio playback
- Android MediaSession integration
- Notification and lock-screen controls
- Headset and Bluetooth transport controls
- Canonical playback queue
- Persistent playlists
- Shuffle and repeat
- A-B Repeat
- Audio Mini Player
- High-quality embedded audio artwork
- Portrait and landscape video playback
- Fullscreen video controls
- Video Audio Only mode
- Child Lock for fullscreen video
- Share and media details
- Android Open With support
- System, Light, and Dark themes
- Media sorting
- Fast scrolling for long lists
- Local preference persistence

## Screenshots

<table>
  <tr>
    <td align="center">
      <strong>Folders</strong><br>
      <img src="docs/assets/screenshots/01-folders.png" width="240" alt="NexPlay Folders">
    </td>
    <td align="center">
      <strong>PlayHub</strong><br>
      <img src="docs/assets/screenshots/02-playhub.png" width="240" alt="NexPlay PlayHub">
    </td>
    <td align="center">
      <strong>Audio Player</strong><br>
      <img src="docs/assets/screenshots/03-audio-player.png" width="240" alt="NexPlay Audio Player">
    </td>
  </tr>
</table>

<table>
  <tr>
    <td align="center">
      <strong>Video Player</strong><br>
      <img src="docs/assets/screenshots/04-video-player.png" width="240" alt="NexPlay Video Player">
    </td>
    <td align="center">
      <strong>Video Fullscreen</strong><br>
      <img src="docs/assets/screenshots/05-video-fullscreen.png" width="420" alt="NexPlay fullscreen video">
    </td>
    <td align="center">
      <strong>Child Lock</strong><br>
      <img src="docs/assets/screenshots/06-child-lock.png" width="420" alt="NexPlay Child Lock">
    </td>
  </tr>
</table>

All screenshots are captured from the actual Android application.
## Download

Official signed NexPlay builds are distributed through GitHub Releases.

NexPlay 1.0.0 is the first public release.

Use release artifacts published from the official NexPlay repository and verify the published SHA-256 checksum when appropriate.

Do not install APK files distributed through unofficial mirrors unless you trust the source.

## Requirements

- Android 6.0 or newer
- Local audio and/or video files
- Media permissions required by the Android version in use

## Technology

NexPlay is built with:

- Kotlin
- Jetpack Compose
- Material 3
- Android Media3 / ExoPlayer
- Kotlin Coroutines
- StateFlow
- Room
- DataStore
- Android MediaStore
- Android MediaSession

## Architecture

NexPlay intentionally keeps media, queue, playback, persistence, and presentation ownership explicit.

```text
MediaStore
    ↓
Media Library
    ↓
Folders / Search / PlayHub / Playlist
    ↓
QueueEngine
    ↓
QueuePlaybackCoordinator
    ↓
PlaybackEngine
    ↓
Single ExoPlayer
    ↓
Audio Player / Video Player / MediaSession
```

Core rules:

- one canonical media-library boundary;
- one authoritative queue;
- one playback engine;
- one ExoPlayer runtime;
- playlist persistence remains separate from runtime queue ownership;
- UI remains a presentation and interaction layer.

See `docs/ARCHITECTURE.md` for details.

## Build from Source

Clone the repository and run commands from the repository root.

### Windows / PowerShell

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug
```

Debug APK:

```text
nexplay_kotlin/app/build/outputs/apk/debug/app-debug.apk
```

See `docs/BUILDING.md` for the complete build workflow.

## Project Structure

```text
NexPlay/
├── nexplay_kotlin/   Native Android application
├── docs/             Public and release documentation
├── devnotes/         Historical development notes
├── AGENTS.md         Engineering workflow rules
└── README.md
```

Local engineering evidence such as `patches/` and `prompt/` is intentionally excluded from Git.

## Privacy

NexPlay is designed around local media playback.

Core media discovery and playback operate against media available to the Android device.

See `docs/PRIVACY.md` for the current privacy model.

## Contributing

Contributions should remain focused and evidence-driven.

Before submitting a change:

1. inspect the relevant implementation and architecture boundaries;
2. keep the change scoped;
3. avoid unrelated refactors;
4. run unit tests;
5. run Android lint;
6. build the application;
7. verify affected runtime behavior.

Engineering workflow details are documented in `AGENTS.md`.

## Development Notes

Historical engineering notes are stored under `devnotes/`.

They are preserved for development context and are not the authoritative description of the current implementation.

## Support NexPlay

Donation options are not currently published.

No donation address or payment account is published.

## Release Notes

Public release history is documented in `CHANGELOG.md`.

The current release is NexPlay 1.0.0.

## License

NexPlay is released under the GNU General Public License v3.0 only (GPL-3.0-only).

Copyright © 2026 FRZDEV.

See LICENSE for details.

---

Built for people who still keep their media library with them.
