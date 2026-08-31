/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.barcode.ar.BarcodeArView
import com.kmp.datacapture.barcode.ui.toAndroidView

// Scanner screen for a single [ArMode]: full-screen camera preview under a black toolbar naming
// the active mode, mirroring the native sample's per-mode ScanFragment layout — black toolbar, no
// back arrow, and a bottom-left circular grey "Return" button instead. `MatrixScanARHost` owns the
// SDK wiring (screen model, lifecycle observer, view embed).
@Composable
internal fun ScannerScreen(mode: ArMode, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = mode.title(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
            )
        }
        // Native pins the camera to a 4:3 container directly below the toolbar and leaves the
        // rest black, with the Return button in that black gap — mirror that instead of a
        // full-bleed preview.
        MatrixScanARHost(
            mode = mode,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            ReturnButton(onClick = onExit, modifier = Modifier.align(Alignment.CenterStart))
        }
    }
}

@Composable
private fun ReturnButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // ic_return already bakes in the grey circle + white glyph (native uses it as the whole
    // ImageButton), so render it directly — no wrapping background circle.
    Column(
        modifier = modifier
            .padding(start = 52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_return),
            contentDescription = "Return",
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Return",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun ArMode.title(): String = when (this) {
    ArMode.Highlights -> "Highlights"
    ArMode.Annotations -> "Annotations"
    ArMode.Popovers -> "Popovers"
    ArMode.StatusIcons -> "Status Icons"
}

@Composable
private fun MatrixScanARHost(mode: ArMode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val screenModel = remember { MatrixScanARScreenModel().also { it.setMode(mode) } }

    val barcodeArView = remember {
        screenModel.registerView(
            BarcodeArView(context, screenModel.barcodeAr, screenModel.buildViewSettings()),
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onResume()
                Lifecycle.Event.ON_PAUSE -> screenModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.dispose()
        }
    }

    // System-bar insets are handled by the outer Column in ScannerScreen (so the toolbar +
    // Return chrome clear the status/nav bars); the camera surface just fills the region between.
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val nativeView: View = barcodeArView.toAndroidView()
            (nativeView.parent as? ViewGroup)?.removeView(nativeView)
            nativeView
        },
    )
}
