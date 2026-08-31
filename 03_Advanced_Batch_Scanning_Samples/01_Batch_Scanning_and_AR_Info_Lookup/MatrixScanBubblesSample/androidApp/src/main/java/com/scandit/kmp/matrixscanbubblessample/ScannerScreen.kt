/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanbubblessample

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.toAndroidView

// Scanner screen: full-screen camera preview under a thin "Scan" top bar, with
// a floating freeze button. The Scaffold gives the back affordance + title;
// `MatrixScanBubblesHost` owns the SDK wiring (screen model, lifecycle
// observer, view embed, bubble factory).
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.LightGrey,
                    titleContentColor = ScanditColors.Black,
                    navigationIconContentColor = ScanditColors.Black,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MatrixScanBubblesHost()
        }
    }
}

@Composable
private fun MatrixScanBubblesHost() {
    val context = LocalContext.current
    val screenModel = remember { MatrixScanBubblesScreenModel() }
    val frozen by screenModel.frozen.collectAsState()

    // We reuse bubble views where possible, keyed on the tracking identifier —
    // same recycling strategy as the native sample's SparseArray<Bubble>.
    val bubbles = remember { SparseArray<Bubble>() }

    // Release cached bubble views once their barcode leaves tracking, so the
    // SparseArray doesn't grow unbounded across a long session.
    remember {
        screenModel.onTrackedBarcodesRemoved = { ids -> ids.forEach { bubbles.remove(it) } }
    }

    val dataCaptureView = remember {
        screenModel.setViewWidth(context.resources.displayMetrics.widthPixels.toFloat())
        screenModel.setupDataCaptureView(
            DataCaptureView(context, screenModel.dataCaptureContext),
        ) { trackedBarcode, bubbleData ->
            val id = trackedBarcode.identifier
            val existing = bubbles.get(id)
            if (existing != null) {
                existing.root
            } else {
                val bubble = Bubble(context, bubbleData, trackedBarcode.barcode.data ?: "")
                bubbles.put(id, bubble)
                bubble.root
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onEvent(MatrixScanBubblesEvent.STARTED)
                Lifecycle.Event.ON_PAUSE -> screenModel.onEvent(MatrixScanBubblesEvent.STOPPED)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.onTrackedBarcodesRemoved = null
            bubbles.clear()
            screenModel.dispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val nativeView: View = dataCaptureView.toAndroidView()
                (nativeView.parent as? ViewGroup)?.removeView(nativeView)
                nativeView
            },
        )

        // Freeze toggle: a 120dp transparent image button centred at the bottom,
        // matching the sample's freeze icons — freeze_enabled (plain circle)
        // while scanning, freeze_disabled (circle with ✕) once frozen.
        Image(
            painter = painterResource(
                if (frozen) R.drawable.freeze_disabled else R.drawable.freeze_enabled,
            ),
            contentDescription = if (frozen) "Resume scanning" else "Freeze",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { screenModel.onEvent(MatrixScanBubblesEvent.TOGGLE_FREEZE) },
        )
    }
}
