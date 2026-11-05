# Restocking Sample

This sample shows how to use `BarcodePick` (MatrixScan Pick) to pick items off a pick list while restocking a shelf:

1. **Pick** — point the camera at the shelf. The built-in `BarcodePickView` highlights every recognized item and lets the user tap to pick or unpick it against the target quantities.
2. **Result** — tapping the view's built-in finish button shows a static breakdown: the **Picklist** (everything picked) and the **Inventory** (everything scanned but not picked).

The cross-platform business logic (mode setup, the in-memory product list, the pick/scan bookkeeping) lives in the Kotlin Multiplatform `shared` module (`RestockingScreenModel`, `ProductManager`). The platform-specific `BarcodePickView` is constructed on each platform host (its constructor differs — Android needs a `Context`, iOS does not) and registered back with the screen model so it can drive the view's lifecycle.

The Android app is built with Jetpack Compose. The iOS app is built with SwiftUI.

## AI Coding Skills

Scandit's AI coding skills teach your coding agent (Claude Code, Cursor, Codex, GitHub Copilot and others) how to pick the right Scandit product and integrate the Data Capture SDK. See [github.com/Scandit/skills](https://github.com/Scandit/skills) for install instructions.

## Building

This sample has no per-module Gradle wrapper. Build it from the repository-root `frameworks/kmp/gradlew` with `-p` pointing at the sample directory:

```bash
JAVA_HOME=~/.sdkman/candidates/java/current ./frameworks/kmp/gradlew \
  -p frameworks/kmp/samples/03_Advanced_Batch_Scanning_Samples/04_Picking/RestockingSample \
  :androidApp:assembleDebug -Pkmp.skipIos=true
```

Enter your Scandit license key in `shared/src/commonMain/kotlin/com/scandit/kmp/restockingsample/LicenseKey.kt`.
