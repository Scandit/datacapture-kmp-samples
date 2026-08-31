/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Two-screen routing for the sample: a landing menu and the scanner. We use a plain `Route`
// sealed hierarchy + Compose state rather than the full Navigation library because two screens
// don't justify the dependency. Customers with more screens should swap this for `NavHost` / a
// type-safe Compose Navigation graph.
private sealed interface Route {
    data object Menu : Route
    data class Scanner(val mode: ArMode) : Route
}

@Composable
internal fun MatrixScanARSimpleApp() {
    var route: Route by remember { mutableStateOf(Route.Menu) }

    when (val current = route) {
        is Route.Menu -> MenuScreen(onSelectMode = { mode -> route = Route.Scanner(mode) })
        is Route.Scanner -> ScannerScreen(mode = current.mode, onExit = { route = Route.Menu })
    }
}
