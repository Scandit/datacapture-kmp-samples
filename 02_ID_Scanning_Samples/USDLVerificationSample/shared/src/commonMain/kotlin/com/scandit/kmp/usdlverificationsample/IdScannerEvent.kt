/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

// User intents and lifecycle signals that the platform host forwards to
// [IdScannerScreenModel.onEvent]. The screen model is the *only* place that
// knows how to translate these into SDK calls.
//
// Modelled as an enum (rather than a sealed interface) because none of the
// events carry payload. UPPER_SNAKE_CASE matches the convention used by
// the rest of the kmp SDK (FrameSourceState, …) and bridges to Swift as
// camelCase — e.g. `IdScannerEvent.dismissResult`.
enum class IdScannerEvent {
    // The host became visible (Activity onResume, SwiftUI onAppear). Turns
    // the camera on and re-arms IdCapture.
    STARTED,

    // The host is no longer visible (Activity onPause, SwiftUI onDisappear).
    // Turns the camera off so the sensor doesn't stay warm in the background.
    STOPPED,

    // The user dismissed the result/error alert. Clears the result and
    // re-arms IdCapture for the next scan.
    DISMISS_RESULT,
}
