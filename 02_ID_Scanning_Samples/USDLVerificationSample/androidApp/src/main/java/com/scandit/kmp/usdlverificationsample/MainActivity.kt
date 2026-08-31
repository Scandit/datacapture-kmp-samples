/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb

// Android entry-point. Intentionally thin — installs the theme, paints the
// status bar to match the Scanner toolbar so there's no visible seam, and
// hands off to `USDLVerificationApp` for routing. Per-screen logic
// lives in `HomeScreen.kt` and `ScannerScreen.kt`.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light Grey scrim with dark icons — keeps the status bar
        // visually continuous with the Scaffold TopAppBar so the top
        // edge reads as a single horizontal band, and replaces the
        // since-deprecated `window.statusBarColor` setter.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                ScanditColors.LightGrey.toArgb(),
                ScanditColors.LightGrey.toArgb(),
            ),
        )
        setContent {
            ScanditTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    USDLVerificationApp()
                }
            }
        }
    }
}
