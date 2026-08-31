/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Routing for the sample: Home -> Search (SparkScan) -> Find (BarcodeFind)
// -> back to Search. A single shared [SearchAndFindScreenModel] owns the SDK
// modes for the whole flow; the rendered screen is derived from its [state].
// We use plain Compose state rather than the Navigation library because the
// handful of linear screens don't justify the dependency.
@Composable
internal fun SearchAndFindApp() {
    val screenModel = remember { SearchAndFindScreenModel() }
    val state by screenModel.state.collectAsState()

    DisposableEffect(screenModel) {
        onDispose { screenModel.dispose() }
    }

    when (val current = state) {
        SearchAndFindUiState.Home -> HomeScreen(
            onStartScan = { screenModel.onEvent(SearchAndFindEvent.NavigateToSearch) },
        )
        is SearchAndFindUiState.Search -> SearchScanScreen(
            screenModel = screenModel,
            scannedItems = current.scannedItems,
            onFind = { screenModel.onEvent(SearchAndFindEvent.NavigateToFind) },
            onExit = { screenModel.onEvent(SearchAndFindEvent.NavigateBack) },
        )
        SearchAndFindUiState.Find -> FindScanScreen(
            screenModel = screenModel,
            // The view's finish button and a back press both end the search and
            // return to Search with the scanned items intact (matches native).
            onExit = { screenModel.onEvent(SearchAndFindEvent.FinishFind) },
        )
    }
}
