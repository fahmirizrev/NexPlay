# Building NexPlay

## Requirements

You need:

- a supported JDK;
- Android SDK;
- Android build tools required by the project;
- Git;
- ADB for physical-device installation and runtime testing.

Android Studio is recommended but not required for command-line builds.

## Repository Root

Run project commands from the NexPlay repository root.

Windows example:

```text
C:\laragon\www\nexplay
```

## Standard Verification

### Unit Tests

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin testDebugUnitTest
```

### Android Lint

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin lintDebug
```

### Debug APK

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebug
```

### Android Test APK

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleDebugAndroidTest
```

### Git Diff Validation

```powershell
git diff --check
```

## Debug APK Location

```text
nexplay_kotlin/app/build/outputs/apk/debug/app-debug.apk
```

## Connected Device

Check available Android devices:

```powershell
adb devices
```

Install the current debug APK:

```powershell
adb install -r nexplay_kotlin\app\build\outputs\apk\debug\app-debug.apk
```

Launch NexPlay:

```powershell
adb shell am force-stop com.nexplay.app
adb shell monkey -p com.nexplay.app -c android.intent.category.LAUNCHER 1
```

## Release Builds

Production release signing is configured through environment variables.

The application expects all four variables below before a signed release is built:

```text
NEXPLAY_RELEASE_STORE_FILE
NEXPLAY_RELEASE_STORE_PASSWORD
NEXPLAY_RELEASE_KEY_ALIAS
NEXPLAY_RELEASE_KEY_PASSWORD
```

`NEXPLAY_RELEASE_STORE_FILE` points to the production keystore stored outside the tracked repository.

Signing passwords and private signing material must never be committed to Git, written into public documentation, or stored in tracked configuration files.

After the signing environment is configured, verify the signing configuration:

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin signingReport --no-configuration-cache
```

Build the signed release APK:

```powershell
.\nexplay_kotlin\gradlew.bat -p nexplay_kotlin assembleRelease --no-configuration-cache
```

Signed release APK:

```text
nexplay_kotlin/app/build/outputs/apk/release/app-release.apk
```

Use `--no-configuration-cache` for production signing tasks so release credentials are not persisted through the Gradle configuration cache.

After the release build completes, clear signing-password environment variables from the active shell.

The following classes of files remain intentionally excluded from Git:

- keystores;
- local signing properties;
- local Android SDK configuration;
- generated build output.
## Release Verification

A public release must be verified from the actual signed release artifact rather than inferred from a debug build.

Release verification includes:

- package identity;
- version metadata;
- signature;
- installation;
- persistence;
- playback;
- MediaSession behavior;
- supported-device runtime smoke tests.