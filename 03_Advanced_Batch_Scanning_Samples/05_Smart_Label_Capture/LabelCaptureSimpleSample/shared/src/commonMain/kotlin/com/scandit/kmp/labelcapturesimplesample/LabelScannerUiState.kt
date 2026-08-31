/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

// The single source of truth for what the scanner screen renders.
//
// State is *immutable*: the screen model emits a new instance each time
// something changes. Customers extending this sample should add new
// variants here rather than mutating fields on an existing one.
sealed interface LabelScannerUiState {

    // Camera is streaming frames and the scanner is looking for a label.
    data object Scanning : LabelScannerUiState

    // A label was just captured (and validated through the validation-flow
    // overlay). The platform host shows the [fields] in a result panel and
    // dispatches [LabelScannerEvent.DISMISS_RESULT] when the user continues.
    data class Scanned(
        val fields: List<LabelFieldResult>,
    ) : LabelScannerUiState
}

// One captured label field rendered as a name/value row. A plain data class
// (rather than a Map) so the list bridges cleanly to SwiftUI.
data class LabelFieldResult(
    val name: String,
    val value: String,
)
