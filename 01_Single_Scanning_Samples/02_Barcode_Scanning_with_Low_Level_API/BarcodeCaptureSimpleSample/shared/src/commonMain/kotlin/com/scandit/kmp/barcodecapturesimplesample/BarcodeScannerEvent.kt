/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodecapturesimplesample

// User intents and lifecycle signals that the platform host forwards to
// [BarcodeScannerScreenModel.onEvent]. The screen model is the *only*
// place that knows how to translate these into SDK calls.
//
// Modelled as an enum (rather than a sealed interface) because none of the
// events carry payload. UPPER_SNAKE_CASE matches the convention used by
// the rest of the kmp SDK (FrameSourceState, Symbology, …) and bridges to
// Swift as camelCase — e.g. `BarcodeScannerEvent.dismissResult`.
enum class BarcodeScannerEvent {
    // The host became visible (Activity onResume, SwiftUI onAppear). Turns
    // the camera on and re-arms BarcodeCapture.
    STARTED,

    // The host is no longer visible (Activity onPause, SwiftUI onDisappear).
    // Turns the camera off so the sensor doesn't stay warm in the background.
    STOPPED,

    // The user dismissed the "Scanned" alert. Clears the result and re-arms
    // BarcodeCapture for the next scan.
    DISMISS_RESULT,
}
