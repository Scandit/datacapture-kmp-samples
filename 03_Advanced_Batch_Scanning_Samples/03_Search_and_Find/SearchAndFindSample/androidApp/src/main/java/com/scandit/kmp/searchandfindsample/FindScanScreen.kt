/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
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
import com.kmp.datacapture.barcode.find.BarcodeFindItem
import com.kmp.datacapture.barcode.find.BarcodeFindView
import com.kmp.datacapture.barcode.find.BarcodeFindViewUiListener
import com.kmp.datacapture.barcode.ui.toAndroidView

// Find phase: a full-screen BarcodeFindView seeded with the barcodes scanned
// during Search. The view owns its own chrome (carousel, hints, shutter and
// finish button); tapping finish (or pressing back) returns to Search with the
// scanned items intact.
@Composable
internal fun FindScanScreen(
    screenModel: SearchAndFindScreenModel,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler(onBack = onExit)

    val barcodeFind = requireNotNull(screenModel.barcodeFindOrNull()) {
        "FindScanScreen entered without a BarcodeFind mode; startFind() must run first"
    }

    val barcodeFindView = remember {
        BarcodeFindView(
            context,
            barcodeFind,
            screenModel.barcodeFindViewSettings,
        ).also { view ->
            view.uiListener = object : BarcodeFindViewUiListener {
                override fun onFinishButtonTapped(foundItems: Set<BarcodeFindItem>) {
                    onExit()
                }
            }
            screenModel.registerBarcodeFindView(
                object : BarcodeFindViewHandle {
                    override fun onResume() = view.onResume()
                    override fun onPause() = view.onPause()
                    override fun startSearching() = view.startSearching()
                    override fun stopSearching() = view.stopSearching()
                },
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onFindResumed()
                Lifecycle.Event.ON_PAUSE -> screenModel.onFindPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.disposeFind()
        }
    }

    // The activity is edge-to-edge; safeDrawingPadding keeps the find view's
    // chrome clear of the status and navigation bars.
    AndroidView(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        factory = {
            val native: View = barcodeFindView.toAndroidView()
            (native.parent as? ViewGroup)?.removeView(native)
            native
        },
    )
}
