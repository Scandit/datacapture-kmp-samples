/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodeselectionsimplesample

// Lifecycle signals the platform host forwards to
// [BarcodeSelectionScreenModel.onEvent]. Switching the Tap/Aim selection
// mode is a separate call ([BarcodeSelectionScreenModel.selectMode])
// because it carries a payload; these two are payload-less.
//
// UPPER_SNAKE_CASE matches the convention used by the rest of the kmp SDK
// (FrameSourceState, Symbology, …) and bridges to Swift as camelCase —
// e.g. `BarcodeSelectionEvent.started`.
enum class BarcodeSelectionEvent {
    // The host became visible (Activity onResume, SwiftUI onAppear). Turns
    // the camera on and re-arms BarcodeSelection.
    STARTED,

    // The host is no longer visible (Activity onPause, SwiftUI onDisappear).
    // Turns the camera off so the sensor doesn't stay warm in the background.
    STOPPED,
}
