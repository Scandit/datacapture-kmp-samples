/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Two-screen routing for the sample: a landing page and the scanner.
// We use a plain `Route` enum + Compose state rather than the full
// Navigation library because two screens don't justify the dependency.
// Customers with more screens should swap this for `NavHost` / a
// type-safe Compose Navigation graph.
internal enum class Route { HOME, SCANNER }

@Composable
internal fun USDLVerificationApp() {
    var route by remember { mutableStateOf(Route.HOME) }

    when (route) {
        Route.HOME -> HomeScreen(onStartScan = { route = Route.SCANNER })
        Route.SCANNER -> ScannerScreen(onExit = { route = Route.HOME })
    }
}
