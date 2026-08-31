/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Two-screen routing: a landing page and the list-building scanner. A plain
// enum + Compose state keeps the sample dependency-free; customers with more
// screens should swap this for Compose Navigation.
internal enum class Route { HOME, LIST_BUILDING }

@Composable
internal fun ListBuildingApp() {
    var route by remember { mutableStateOf(Route.HOME) }

    when (route) {
        Route.HOME -> HomeScreen(onStartScan = { route = Route.LIST_BUILDING })
        Route.LIST_BUILDING -> ListBuildingScreen(onExit = { route = Route.HOME })
    }
}
