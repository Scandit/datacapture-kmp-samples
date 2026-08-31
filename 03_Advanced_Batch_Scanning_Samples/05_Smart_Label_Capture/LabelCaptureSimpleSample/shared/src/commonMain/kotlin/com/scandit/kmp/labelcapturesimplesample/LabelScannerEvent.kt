/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

// User intents and lifecycle signals the platform host forwards to
// [LabelScannerScreenModel.onEvent]. The screen model is the *only* place
// that knows how to translate these into SDK calls.
//
// UPPER_SNAKE_CASE matches the kmp SDK convention (FrameSourceState,
// Symbology, …) and bridges to Swift as camelCase — e.g.
// `LabelScannerEvent.dismissResult`.
enum class LabelScannerEvent {
    // The host became visible (Activity onResume, SwiftUI onAppear). Turns
    // the camera on, re-arms LabelCapture, and resumes the validation flow.
    STARTED,

    // The host is no longer visible (Activity onPause, SwiftUI onDisappear).
    // Pauses the validation flow and turns the camera off so the sensor
    // doesn't stay warm in the background.
    STOPPED,

    // The user dismissed the captured-label result. Clears the result and
    // re-arms LabelCapture + the camera for the next scan.
    DISMISS_RESULT,
}
