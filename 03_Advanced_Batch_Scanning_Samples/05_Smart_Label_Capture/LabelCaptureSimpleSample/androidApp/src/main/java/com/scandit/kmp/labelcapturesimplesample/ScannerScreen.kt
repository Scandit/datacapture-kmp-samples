/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.toAndroidView

// Scanner screen: full-screen camera preview under a thin "Scan" top bar.
// The Scaffold gives us the back affordance + title; `LabelScannerHost`
// owns the actual SDK wiring (screen model, lifecycle observer, view
// embed).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                // Light Grey on Scandit Black instead of solid black — gives
                // a subtle, on-brand divider above the camera preview without
                // a heavy bar dominating the screen.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.LightGrey,
                    titleContentColor = ScanditColors.Black,
                    navigationIconContentColor = ScanditColors.Black,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LabelScannerHost()
        }
    }
}

@Composable
private fun LabelScannerHost() {
    val context = LocalContext.current
    val screenModel = remember { LabelScannerScreenModel() }
    val state by screenModel.state.collectAsState()

    val dataCaptureView = remember {
        screenModel.setupDataCaptureView(
            DataCaptureView(context, screenModel.dataCaptureContext),
        )
    }

    // Two concerns to handle here:
    //  1. Activity background/foreground (system pause/resume) — forward
    //     to the validation-flow overlay, which pauses/restores the camera
    //     itself (preserving a paused state across backgrounding).
    //  2. Composition leave (user navigates back to home) — Activity stays
    //     RESUMED, so ON_PAUSE never fires; we still need to stop the
    //     camera explicitly in `onDispose`.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onEvent(LabelScannerEvent.STARTED)
                Lifecycle.Event.ON_PAUSE -> screenModel.onEvent(LabelScannerEvent.STOPPED)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Tear down every SDK resource the model owns — also turns the
            // camera off. STOPPED alone would just disable the mode and
            // leave it attached to the shared DataCaptureContext; over
            // multiple visits those modes would compound and slow scanning.
            screenModel.dispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Pattern 7 (slot): the shared model produces the SDK view; the host
        // is the only place that knows how to embed an Android View in a
        // Compose tree.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val nativeView: View = dataCaptureView.toAndroidView()
                (nativeView.parent as? ViewGroup)?.removeView(nativeView)
                nativeView
            },
        )

        val scanned = state as? LabelScannerUiState.Scanned
        if (scanned != null) {
            AlertDialog(
                onDismissRequest = { screenModel.onEvent(LabelScannerEvent.DISMISS_RESULT) },
                confirmButton = {
                    TextButton(
                        onClick = { screenModel.onEvent(LabelScannerEvent.DISMISS_RESULT) },
                    ) { Text("Continue") }
                },
                title = { Text("Captured Label") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scanned.fields.forEach { field ->
                            Text("${field.name}: ${field.value}")
                        }
                    }
                },
            )
        }
    }
}
