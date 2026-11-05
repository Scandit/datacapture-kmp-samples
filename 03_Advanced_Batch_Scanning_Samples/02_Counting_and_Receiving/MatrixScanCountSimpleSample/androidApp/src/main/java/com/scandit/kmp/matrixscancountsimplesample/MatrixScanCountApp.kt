/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Which "Done" semantics the Results screen should use — mirrors the native
 * sample's `ResultsActivity.DoneButtonStyle`.
 *
 *  - [RESUME]: reached via the list button. Done just navigates back; the
 *    scanning session is untouched.
 *  - [NEW_SCAN]: reached via the exit button. Done resets the session (clears
 *    results, resets the mode) and navigates back to a fresh Scanner.
 */
internal enum class DoneButtonStyle { RESUME, NEW_SCAN }

private sealed interface Route {
    data object Home : Route
    data object Scanner : Route
    data class Results(val doneButtonStyle: DoneButtonStyle) : Route
}

/**
 * App-level routing. Starts on the [HomeScreen] landing page (matching every
 * other KMP sample); the Home button drives the camera-permission flow and
 * then navigates into the Scanner. The Scanner keeps its `BarcodeCountView`
 * composed with Results drawn as an opaque overlay on top.
 */
@Composable
internal fun MatrixScanCountApp(screenModel: MatrixScanCountScreenModel) {
    var route by remember { mutableStateOf<Route>(Route.Home) }

    when (route) {
        is Route.Home -> HomeScreen(onStartScan = { route = Route.Scanner })

        else -> {
            // Device back on the Scanner returns to Home (Results handles its
            // own back via the app-bar up action / BackHandler).
            if (route is Route.Scanner) {
                BackHandler { route = Route.Home }
            }

            // Keep the Scanner (and its BarcodeCountView) composed and draw
            // Results as an opaque overlay on top — mirrors the native sample,
            // where MainActivity retains BarcodeCountView while ResultsActivity
            // is shown. Swapping it out would recreate the view (and restart the
            // camera) on return, which flickers.
            Box(modifier = Modifier.fillMaxSize()) {
                ScannerScreen(
                    screenModel = screenModel,
                    onListTap = { route = Route.Results(DoneButtonStyle.RESUME) },
                    onExitTap = { route = Route.Results(DoneButtonStyle.NEW_SCAN) },
                )

                (route as? Route.Results)?.let { current ->
                    ResultsScreen(
                        screenModel = screenModel,
                        doneButtonStyle = current.doneButtonStyle,
                        onDone = { route = Route.Scanner },
                    )
                }
            }

            // Camera follows the Activity lifecycle, not just the composition:
            // backgrounding the app never disposes this subtree, so a
            // composition-scoped effect would leave the camera unmanaged across
            // minimize/reopen (black preview on return). ON_RESUME/ON_PAUSE keep
            // it in step. The explicit resume after registering covers the case
            // where the subtree is composed before the Activity reaches RESUMED;
            // it is idempotent, so the replay `addObserver` performs when the
            // Activity is already RESUMED costs nothing. ON_PAUSE doesn't fire on
            // a back-navigation to Home while the Activity stays RESUMED, so
            // onDispose pauses too.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> screenModel.resumeFrameSource()
                        Lifecycle.Event.ON_PAUSE -> screenModel.pauseFrameSource()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                screenModel.resumeFrameSource()
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    screenModel.pauseFrameSource()
                }
            }

            // Freeze the results list while Results is shown so a Clear List
            // isn't instantly refilled by the still-running scanner. Back on the
            // Scanner, resume live updates and re-enable the mode + camera (the
            // pre-built exit button disables the mode by default).
            LaunchedEffect(route) {
                if (route is Route.Scanner) {
                    screenModel.resumeFrameSource()
                    screenModel.setUpdatesEnabled(true)
                } else {
                    screenModel.setUpdatesEnabled(false)
                }
            }
        }
    }
}
