/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

// The single source of truth for what the app renders. The screen model emits
// a new immutable instance whenever something changes; the platform host
// renders the matching screen.
//
// Two-phase flow (mirrors the native SearchAndFindSample):
//   Home   -> landing page
//   Search -> SparkScan: scan the barcodes you want to find later
//   Find   -> BarcodeFind: point the camera at the scene to locate them
// Finishing Find returns to Search with the scanned items intact (no result
// screen — matches the native sample's onBackPressed behaviour).
sealed interface SearchAndFindUiState {

    // Landing page.
    data object Home : SearchAndFindUiState

    // SparkScan phase. [scannedItems] backs both the toolbar counter and the
    // scanned-barcode list (mirrors the native sample's RecyclerView).
    data class Search(
        val scannedItems: List<SearchScannedBarcode>,
    ) : SearchAndFindUiState {
        val scannedCount: Int get() = scannedItems.size
    }

    // BarcodeFind phase. The host embeds the BarcodeFindView; tapping its
    // built-in finish button (or a back press) returns to Search.
    data object Find : SearchAndFindUiState
}

// One scanned barcode rendered in the Search list: its data, the readable
// symbology name, and the cropped thumbnail(s) of the scan (empty when the
// frame carried no image — at most one here, since every scan is its own row
// and duplicates are NOT grouped). Modelled as a list (rather than a nullable
// single image) to match ListBuildingSample's Swift-bridged shape.
data class SearchScannedBarcode(
    val data: String,
    val symbology: String,
    val images: List<CroppedImage> = emptyList(),
)
