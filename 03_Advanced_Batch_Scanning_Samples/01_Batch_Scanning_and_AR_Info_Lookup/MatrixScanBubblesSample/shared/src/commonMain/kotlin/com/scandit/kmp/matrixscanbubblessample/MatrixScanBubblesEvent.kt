/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanbubblessample

// User intents and lifecycle signals that the platform host forwards to
// [MatrixScanBubblesScreenModel.onEvent]. The screen model is the *only*
// place that knows how to translate these into SDK calls.
//
// Modelled as an enum (rather than a sealed interface) because none of the
// events carry payload. UPPER_SNAKE_CASE matches the convention used by
// the rest of the kmp SDK (FrameSourceState, Symbology, …) and bridges to
// Swift as camelCase — e.g. `MatrixScanBubblesEvent.toggleFreeze`.
enum class MatrixScanBubblesEvent {
    // The host became visible (Activity onResume, SwiftUI onAppear). Turns the
    // camera on and re-arms BarcodeBatch. Ignored while frozen so the frozen
    // frame and its bubbles survive a background/foreground round-trip.
    STARTED,

    // The host is no longer visible (Activity onPause, SwiftUI onDisappear).
    // Turns the camera off so the sensor doesn't stay warm in the background.
    // Ignored while frozen (nothing is streaming anyway).
    STOPPED,

    // The freeze button was tapped. Toggles between live scanning and a frozen
    // frame: freezing pauses BarcodeBatch + stops the camera while keeping the
    // currently-tracked bubbles; unfreezing resumes the camera (which clears
    // the bubbles as new frames arrive).
    TOGGLE_FREEZE,
}
