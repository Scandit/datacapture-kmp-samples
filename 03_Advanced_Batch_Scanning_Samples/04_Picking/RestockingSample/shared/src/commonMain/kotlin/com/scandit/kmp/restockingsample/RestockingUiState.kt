/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

// The single source of truth for what the app renders. The screen model emits
// a new immutable instance whenever something changes; the platform host
// renders the matching screen.
//
// Two-phase flow (mirrors the native RestockingSample):
//   Pick   -> BarcodePick: point the camera at the shelf and pick items off
//             the pick list.
//   Result -> a static list of what was picked (Picklist) and what was
//             scanned but not picked (Inventory), shown after the built-in
//             finish button is tapped.
sealed interface RestockingUiState {

    // Picking phase: the host embeds the BarcodePickView.
    data object Pick : RestockingUiState

    // Result phase, built from [ProductManager.getPickResultContent].
    data class Result(val items: List<ListItem>) : RestockingUiState
}
