/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodeselectionsimplesample

// The single source of truth for what the scan screen renders.
//
// State is *immutable*: the screen model emits a new instance each time
// something changes. [selectionMode] drives the bottom Tap/Aim switch;
// [lastSelection] + [selectionEventId] deliver the transient "just
// selected" toast — the host shows a snackbar whenever [selectionEventId]
// increments (BarcodeSelection re-selecting the same barcode bumps its
// count but produces an equal [lastSelection], so the monotonic id, not
// value equality, is what signals a fresh event).
data class BarcodeSelectionUiState(
    val selectionMode: SelectionMode = SelectionMode.TAP,
    val lastSelection: SelectedBarcode? = null,
    val selectionEventId: Long = 0L,
)

// The two ways the user can select barcodes in this sample, mapped 1:1 to
// the bottom-bar tabs. Switching applies a new selection type to the
// running mode.
enum class SelectionMode {
    TAP,
    AIMER,
}

// One just-selected barcode plus how many times BarcodeSelection has
// counted it selected so far ([count]).
data class SelectedBarcode(
    val data: String,
    val symbologyName: String,
    val count: Int,
)
