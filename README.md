# Scandit Data Capture SDK — Kotlin Multiplatform Mobile Samples

This directory holds the customer-facing samples for the KMP bindings of the Scandit Data Capture SDK. The internal `DebugApp` at `../DebugApp/` is the day-to-day test harness; samples here are the minimal, focused references shipped alongside each release.

| Sample                                                                                                                     | Category                | Description                                                                                                                 |
| -------------------------------------------------------------------------------------------------------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| [BarcodeCaptureSimpleSample](01_Single_Scanning_Samples/02_Barcode_Scanning_with_Low_Level_API/BarcodeCaptureSimpleSample) | Single Scanning         | Basic sample that uses the camera to read a single barcode.                                                                 |
| [MatrixScanCountSimpleSample](03_Advanced_Batch_Scanning_Samples/02_Counting_and_Receiving/MatrixScanCountSimpleSample)    | Advanced Batch Scanning | Demonstrates the pre-built MatrixScan Count component (BarcodeCount API) to batch scan barcodes for receiving and counting. |

Each sample is a standalone Gradle build with its own `settings.gradle.kts`. Open the sample's directory directly in Android Studio (or `./gradlew :androidApp:assembleDebug` from the sample root); the iOS app lives under `iosApp/` and consumes the Scandit native xcframeworks through a local Swift Package.

## AI Coding Skills

Scandit's AI coding skills teach your coding agent (Claude Code, Cursor, Codex, GitHub Copilot and others) how to pick the right Scandit product and integrate the Data Capture SDK. See [github.com/Scandit/skills](https://github.com/Scandit/skills) for install instructions.
