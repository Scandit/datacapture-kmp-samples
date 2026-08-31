/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

// One-shot signals the [SearchAndFindScreenModel] emits to the platform host
// (navigation + transient errors). State that the screen *renders* lives in
// [SearchAndFindUiState]; this channel is for things that happen once.
//
// UpperCamelCase data objects bridge to Swift as enum-like singletons — e.g.
// `SearchAndFindEvent.NavigateToFind`.
sealed interface SearchAndFindEvent {

    // Move from the landing page into the Search (SparkScan) phase.
    data object NavigateToSearch : SearchAndFindEvent

    // The user tapped FIND: leave Search and build the Find (BarcodeFind)
    // phase from the barcodes scanned so far.
    data object NavigateToFind : SearchAndFindEvent

    // BarcodeFind finished (finish button or back press): return to Search
    // with the scanned items intact.
    data object FinishFind : SearchAndFindEvent

    // Pop one step back toward Home.
    data object NavigateBack : SearchAndFindEvent

    // A recoverable error the host should surface.
    data class ShowError(val message: String) : SearchAndFindEvent
}
