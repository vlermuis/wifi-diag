# Wi‑Fi Diagnostics

A small, dependency-light Android app that displays the current Wi‑Fi connection and runs a basic internet reachability test.

## Included diagnostics

- Wi‑Fi connection state
- SSID and BSSID, when Android allows access
- Signal strength in dBm
- Link speed and frequency
- Local IP address and default gateway
- Android network validation status
- Timed HTTPS reachability test

## Build

Open this folder in Android Studio and let it sync the Gradle project. The app targets Android API 35 and supports Android 8.0 (API 26) and newer.

## Build an APK without Android Studio

The included `.github/workflows/build-apk.yml` builds a debug APK automatically on GitHub. Upload this project to a GitHub repository, open the **Actions** tab, select **Build APK**, and choose **Run workflow**. The resulting `app-debug.apk` is available from the workflow run’s Artifacts section.

The app requests the Android permissions needed to read Wi‑Fi identity information. On newer Android versions, the OS may still redact SSID/BSSID unless Location is enabled.
