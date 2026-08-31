/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Two-phase routing for the sample: a landing page (HomeScreen) that handles
// the camera-permission flow, then the pick/result flow driven by the shared
// [RestockingScreenModel]. We use a plain `Route` enum + Compose state rather
// than the Navigation library because the flow is small. Mirrors the other
// KMP advanced-batch samples.
internal enum class Route { HOME, SCANNER }

@Composable
internal fun RestockingApp() {
    val screenModel = remember { RestockingScreenModel() }
    var route by remember { mutableStateOf(Route.HOME) }

    DisposableEffect(screenModel) {
        onDispose { screenModel.dispose() }
    }

    when (route) {
        Route.HOME -> HomeScreen(onStartScan = { route = Route.SCANNER })
        Route.SCANNER -> ScannerFlow(
            screenModel = screenModel,
            // "Finish pickup" clears the session and returns to the landing
            // page; starting a new scan begins a fresh picking session.
            onFinishPickup = {
                screenModel.onEvent(RestockingEvent.FinishPickup)
                route = Route.HOME
            },
        )
    }
}

@Composable
private fun ScannerFlow(screenModel: RestockingScreenModel, onFinishPickup: () -> Unit) {
    val state by screenModel.state.collectAsState()

    when (val current = state) {
        RestockingUiState.Pick -> PickScreen(screenModel = screenModel)
        is RestockingUiState.Result -> ResultScreen(
            items = current.items,
            onContinueScanning = { screenModel.onEvent(RestockingEvent.ContinueScanning) },
            onFinishPickup = onFinishPickup,
        )
    }
}
