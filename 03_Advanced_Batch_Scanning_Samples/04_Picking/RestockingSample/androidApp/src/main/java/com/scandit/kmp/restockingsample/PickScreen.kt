/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.barcode.pick.BarcodePickView
import com.kmp.datacapture.barcode.ui.toAndroidView

// Pick phase: a full-screen BarcodePickView. The view owns its own chrome
// (highlights, hints, finish button); tapping the built-in finish button
// fires [RestockingScreenModel.onFinishButtonTapped] via the mode's own
// BarcodePickViewUiListener registration, moving to the Result screen.
// Mirrors FindScanScreen in the sibling SearchAndFindSample.
@Composable
internal fun PickScreen(screenModel: RestockingScreenModel) {
    val context = LocalContext.current

    val barcodePickView = remember {
        BarcodePickView(
            context,
            screenModel.barcodePick,
            screenModel.barcodePickViewSettings,
        ).also { view ->
            view.uiListener = screenModel
            view.addActionListener(screenModel)
            screenModel.registerBarcodePickView(
                object : BarcodePickViewHandle {
                    override fun start() = view.start()
                    override fun onResume() = view.onResume()
                    override fun onPause() = view.onPause()
                    override fun stop() = view.stop()
                },
            )
        }
    }

    // start() is the final mount step (matches the compose barcode-compose module's
    // BarcodePickView wrapper); stop() is a terminal teardown, called once on dispose.
    DisposableEffect(barcodePickView) {
        barcodePickView.start()
        onDispose {
            barcodePickView.stop()
            screenModel.unregisterBarcodePickView()
        }
    }

    // Resume/pause the view lifecycle with the Activity. The observer won't replay
    // ON_RESUME if the Activity is already RESUMED at mount, so call onPickResumed()
    // directly after registering. ON_PAUSE likewise doesn't fire on a back-navigation
    // while the Activity stays RESUMED, so onDispose pauses the lifecycle too.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onPickResumed()
                Lifecycle.Event.ON_PAUSE -> screenModel.onPickPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        screenModel.onPickResumed()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.onPickPaused()
        }
    }

    // safeDrawingPadding keeps the BarcodePickView's own bottom controls
    // (pause / finish) and top guidance clear of the system bars. A bare
    // AndroidView host under enableEdgeToEdge receives no window insets, so
    // without this the controls render under the navigation bar (a Scaffold
    // would apply this automatically; this screen has no Scaffold).
    AndroidView(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        factory = {
            val native: View = barcodePickView.toAndroidView()
            (native.parent as? ViewGroup)?.removeView(native)
            native
        },
    )
}
