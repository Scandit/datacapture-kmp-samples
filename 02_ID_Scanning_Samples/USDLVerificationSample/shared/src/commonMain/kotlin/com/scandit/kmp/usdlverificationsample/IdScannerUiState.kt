/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

import com.kmp.datacapture.core.ui.CapturedImage

// The single source of truth for what the scanner screen renders.
//
// State is *immutable*: the screen model emits a new instance each time
// something changes. Customers extending this sample should add new
// variants here rather than mutating fields on an existing one — that's
// what makes the state predictable across platforms.
//
// The screen model maps [CapturedId] / [RejectionReason] into the
// platform-independent structures below; each platform host renders them
// natively (Compose on Android, SwiftUI on iOS) into the rich result screen
// that mirrors the native Android USDLVerificationSample — a captured front
// image, per-check verification tiles, and the full field list.
sealed interface IdScannerUiState {

    // Camera is streaming frames and IdCapture is looking for a document.
    data object Scanning : IdScannerUiState

    // A document reached a verification verdict — either fully accepted
    // ([rejection] == null, three green tiles) or rejected by one of the
    // verification rules ([rejection] set, a single red tile). Both render
    // through the same rich screen, mirroring the native sample.
    data class Result(
        val title: String,
        // Captured front-side / face image, when the SDK surfaced one.
        val image: CapturedImage?,
        // Per-check verification tiles (expiry / data-consistency / barcode).
        val checks: List<VerificationCheck>,
        // Full field list (label → value), always populated; missing values
        // render as [EMPTY_VALUE] to match the native sample.
        val fields: List<Field>,
    ) : IdScannerUiState

    // A document was localized but rejected for a non-verification reason
    // (unsupported type, bad format, capture timeout). [message] explains why;
    // these don't carry verification tiles or an image, mirroring the native
    // sample's separate rejected-document path.
    data class Rejected(
        val message: String,
    ) : IdScannerUiState

    // One verification check shown as a tile. [passed] drives the green/red
    // styling and icon on each platform.
    data class VerificationCheck(
        val label: String,
        val passed: Boolean,
    )

    // One label/value row in the field list.
    data class Field(
        val label: String,
        val value: String,
    )

    companion object {
        // Placeholder shown for an empty/absent field, matching the native
        // sample's ResultMapper.EMPTY_TEXT_VALUE.
        const val EMPTY_VALUE: String = "<empty>"
    }
}
