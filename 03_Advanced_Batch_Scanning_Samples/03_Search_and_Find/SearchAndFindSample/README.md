# Search & Find Sample

This sample shows a consecutive use of two barcode-scanning modes in a single app:

1. **Search** — use `SparkScan` to scan the set of barcodes you want to find later. Each scanned barcode is added to a list; a counter in the toolbar shows how many you have so far, and a **CLEAR LIST** action resets it.
2. **Find** — tap **FIND** and the scanned barcodes seed a `BarcodeFind` (MatrixScan Find) session. Point the camera at your environment and the built-in `BarcodeFindView` UI highlights the items as you locate them.
3. **Summary** — when you finish the Find session, the app shows how many of the searched items were found.

The cross-platform business logic (mode setup, the scanned-barcode list, phase transitions) lives in the Kotlin Multiplatform `shared` module (`SearchAndFindScreenModel`). The platform-specific views (`SparkScanView`, `BarcodeFindView`) are constructed on the Android side (their constructors need an Android `Context`) and registered back with the screen model so it can drive their lifecycle.

The Android app is built with Jetpack Compose. The iOS app is provided as a skeleton.

## AI Coding Skills

Scandit's AI coding skills teach your coding agent (Claude Code, Cursor, Codex, GitHub Copilot and others) how to pick the right Scandit product and integrate the Data Capture SDK. See [github.com/Scandit/skills](https://github.com/Scandit/skills) for install instructions.

## Building

This sample has no per-module Gradle wrapper. Build it from the repository-root `frameworks/kmp/gradlew` with `-p` pointing at the sample directory:

```bash
JAVA_HOME=~/.sdkman/candidates/java/current ./frameworks/kmp/gradlew \
  -p frameworks/kmp/samples/03_Advanced_Batch_Scanning_Samples/03_Search_and_Find/SearchAndFindSample \
  :androidApp:assembleDebug -Pkmp.skipIos=true
```

Enter your Scandit license key in `shared/src/commonMain/kotlin/com/scandit/kmp/searchandfindsample/LicenseKey.kt`.
