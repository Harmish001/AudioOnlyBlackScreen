# AudioOnly Black Screen (Android phone + Android TV starter)

A native Kotlin Android Studio project for a parent-controlled, full-screen black overlay intended to let audio continue while hiding the video behind it.

## What is implemented

- Android phone launcher UI.
- Android TV / Google TV launcher entry (`LEANBACK_LAUNCHER`) and touchscreen-optional manifest features.
- Full-screen black overlay using Android's application-overlay window API.
- Mobile double-tap gesture to unhide the screen (with PIN verification).
- Android TV remote-navigable Cross Button (✕) with focused visual feedback to unhide the screen.
- Parent PIN setup on first launch (4–8 digits).
- PIN-gated unlock screen that pauses overlay while typing PIN and resumes if dismissed.
- Persistent foreground-service notification that opens the PIN unlock screen.
- Overlay permission flow and basic handling for TV firmware that refuses overlays.

## Important platform limitation

This app does **not** play or extract audio from YouTube. It covers the screen of another app and leaves that app running underneath. Whether audio continues depends on the source app, Android version, device firmware, and playback state. Android TV manufacturers often restrict overlays; on those devices the overlay may not appear. This must be tested on target devices before claiming universal TV support. A standalone black-screen media player can be added as a fallback, but it will only play media the app is permitted to play.

The overlay consumes accidental touches while active. On mobile, double-tap anywhere to prompt for PIN and unhide. On Android TV, navigate to the top-corner Cross Button (✕) using the remote D-pad and press OK to enter PIN and unhide.

## Build an APK

1. Install Android Studio (current stable) and a JDK 17 runtime.
2. Open this folder (`AudioOnlyBlackScreen`) in Android Studio.
3. Allow Gradle sync and install Android SDK Platform 35 if prompted.
4. Connect an Android phone or emulator. For Android TV, use an Android TV emulator or target TV.
5. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Android Studio will show the output location, typically `app/build/outputs/apk/debug/app-debug.apk`.

The ZIP does not include a prebuilt APK because Android SDK/Gradle builds must be performed in an Android build environment.

## Test on a phone

1. Install and open the app.
2. Set a parent PIN when prompted.
3. Tap **Allow overlay permission** and grant the special permission in Android Settings.
4. Start audio in another app and return to AudioOnly.
5. Tap **Start black screen**.
6. Double-tap anywhere on the black screen (or tap the AudioOnly ongoing notification) and enter the parent PIN to restore the screen.

## Test on Android TV / Google TV

1. Build and install the APK on an Android TV emulator/device.
2. Try enabling the overlay permission if the TV firmware offers it.
3. Start audio in a compatible app, return to AudioOnly, and activate the overlay.
4. Use the TV remote D-pad to navigate to the cross button (✕) in the top-right corner.
5. Press D-pad Center / OK to trigger the PIN unlock prompt and restore video.
4. Verify audio continuity, remote navigation, notification availability, and PIN unlock behavior on that exact device.

## Project structure

```text
AudioOnlyBlackScreen/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/audioonly/blackscreen/
│       │   ├── MainActivity.kt
│       │   ├── BlackOverlayService.kt
│       │   └── UnlockActivity.kt
│       └── res/values/styles.xml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Before public release

- Test on the Android versions and TV brands you intend to support.
- Add a TV-specific fallback for devices that prohibit overlays.
- Add a parent PIN change/reset flow and stronger lockout policy.
- Review Google Play policies for special app access and foreground-service use.
- Consider accessibility, remote-control UX, and audio behavior for every target player.
