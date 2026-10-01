/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat

// Android entry-point. Intentionally thin — installs the theme, paints the
// status bar to match the toolbar so there's no visible seam, owns the
// single long-lived MatrixScanCountScreenModel for the Activity's lifetime,
// and hands off to `MatrixScanCountApp` for routing.
class MainActivity : ComponentActivity() {

    private val screenModel by lazy { MatrixScanCountScreenModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Match the native sample's window model: the decor fits the system
        // windows (NOT edge-to-edge), so the content sits below the status bar
        // and above the navigation bar. The pre-built BarcodeCountView then
        // fills that content area and lands its bottom controls just above the
        // nav bar — exactly like native — without any manual inset padding.
        // Paint the status bar black to match the native AppTheme
        // (statusBarColor = colorPrimary #000000) with light icons.
        window.statusBarColor = Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        setContent {
            ScanditTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MatrixScanCountApp(screenModel)
                }
            }
        }
    }

    override fun onDestroy() {
        screenModel.dispose()
        super.onDestroy()
    }
}
