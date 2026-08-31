/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodecapturesimplesample

// The single source of truth for what the scanner screen renders.
//
// State is *immutable*: the screen model emits a new instance each time
// something changes. Customers extending this sample should add new
// variants here rather than mutating fields on an existing one — that's
// what makes the state predictable across platforms.
sealed interface BarcodeScannerUiState {

    // Camera is streaming frames and the scanner is looking for a barcode.
    data object Scanning : BarcodeScannerUiState

    // A barcode was just recognised. The platform host shows an alert
    // populated from [data] / [symbologyName] and dispatches
    // [BarcodeScannerEvent.DismissResult] when the user acknowledges.
    data class Scanned(
        val data: String,
        val symbologyName: String,
    ) : BarcodeScannerUiState
}
