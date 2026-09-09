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
- Nearby Wi‑Fi scan with up to 50 access points, sorted by signal strength
- Per-access-point SSID, BSSID, RSSI, explicit band/channel/frequency, channel width, center frequency, WPA/WPA2/WPA3 security classification plus raw capabilities, Wi‑Fi standard, scan age, Passpoint, 802.11mc, and operator metadata when available

## Build

Open this folder in Android Studio and let it sync the Gradle project. The app targets Android API 35 and supports Android 8.0 (API 26) and newer.

## Build an APK without Android Studio

The included `.github/workflows/build-apk.yml` builds a debug APK automatically on GitHub. Upload this project to a GitHub repository, open the **Actions** tab, select **Build APK**, and choose **Run workflow**. The resulting `app-debug.apk` is available from the workflow run’s Artifacts section.

The app requests the Android permissions needed to read Wi‑Fi identity information. On newer Android versions, the OS may still redact SSID/BSSID unless Location is enabled.

Android controls scan frequency and may return cached results or reject a scan to protect battery life. The app therefore reports the scan result Android provides rather than attempting to bypass those limits.

The scan path also checks that Wi‑Fi and Location services are enabled and handles permission or scan-throttling failures in the UI instead of terminating the app.
