/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.idcapturesimplesample

// The single source of truth for what the scanner screen renders.
//
// State is *immutable*: the screen model emits a new instance each time
// something changes. Customers extending this sample should add new
// variants here rather than mutating fields on an existing one — that's
// what makes the state predictable across platforms.
//
// The screen model pre-renders the human-readable strings ([Captured.fields]
// / [Rejected.message]) so each platform host just shows them in an alert —
// no platform-specific formatting of [CapturedId] / [RejectionReason].
sealed interface IdScannerUiState {

    // Camera is streaming frames and IdCapture is looking for a document.
    data object Scanning : IdScannerUiState

    // A document was captured. [fields] is the multi-line summary
    // (full name, dates, document number, …) shown in the "Recognized
    // Document" alert.
    data class Captured(
        val fields: String,
    ) : IdScannerUiState

    // A document was localized but rejected (unsupported type, bad format,
    // a rejection rule, or capture timeout). [message] explains why.
    data class Rejected(
        val message: String,
    ) : IdScannerUiState
}
