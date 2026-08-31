/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodeselectionsimplesample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.toAndroidView

// Scan screen: full-screen camera preview with a bottom bar that switches
// between the two selection strategies (Tap-to-Select / Aim-to-Select).
// Each newly selected barcode is surfaced as a transient snackbar.
// `BarcodeSelectionScreenModel` owns all SDK wiring.
@Composable
internal fun ScannerScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val screenModel = remember { BarcodeSelectionScreenModel() }
    val state by screenModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val dataCaptureView = remember {
        screenModel.setupDataCaptureView(
            DataCaptureView(context, screenModel.dataCaptureContext),
        )
    }

    // Two concerns: (1) Activity background/foreground must stop the camera;
    // (2) leaving the composition (back to home) keeps the Activity RESUMED
    // so ON_PAUSE never fires — dispose() below still tears everything down.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onEvent(BarcodeSelectionEvent.STARTED)
                Lifecycle.Event.ON_PAUSE -> screenModel.onEvent(BarcodeSelectionEvent.STOPPED)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.dispose()
        }
    }

    // Show a transient toast whenever the model reports a fresh selection.
    // Keyed on the monotonic event id so re-selecting the same barcode
    // (which only bumps the count) still fires.
    LaunchedEffect(state.selectionEventId) {
        val selection = state.lastSelection
        if (state.selectionEventId > 0L && selection != null) {
            snackbarHostState.showSnackbar(
                message = "${selection.symbologyName}: ${selection.data}\nTimes: ${selection.count}",
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectionMode == SelectionMode.TAP,
                    onClick = { screenModel.selectMode(SelectionMode.TAP) },
                    icon = {},
                    label = { Text("Tap to Select") },
                )
                NavigationBarItem(
                    selected = state.selectionMode == SelectionMode.AIMER,
                    onClick = { screenModel.selectMode(SelectionMode.AIMER) },
                    icon = {},
                    label = { Text("Aim to Select") },
                )
            }
        },
    ) { padding ->
        // Slot pattern: the shared model produces the SDK view; the host is
        // the only place that knows how to embed an Android View in Compose.
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = {
                val nativeView: View = dataCaptureView.toAndroidView()
                (nativeView.parent as? ViewGroup)?.removeView(nativeView)
                nativeView
            },
        )
    }
}
