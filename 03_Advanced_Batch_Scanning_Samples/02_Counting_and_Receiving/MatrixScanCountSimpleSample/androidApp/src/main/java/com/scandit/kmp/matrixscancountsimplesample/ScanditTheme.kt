/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Scandit brand palette (see Scandit_Brand_Colors_2025).
internal object ScanditColors {
    val Black = Color(0xFF141F2E)
    val White = Color(0xFFFFFFFF)
    val Cyan = Color(0xFF2EC1CE)
    val LightGrey = Color(0xFFF1F5F8)
    val Blue = Color(0xFF0252B5)

    // Result-row chrome (mirrors the native sample): grey thumbnail placeholder
    // and muted subtitle text.
    val DarkGrey = Color(0xFFAAAAAA)
    val SubtitleGrey = Color(0xFF8795A1)

    // Pure black for the system bars and app bars, matching the native sample's
    // colorPrimary (#000000) — distinct from the brand navy [Black] so the
    // status bar, title bar and BarcodeCountView control panel share one tone.
    val ChromeBlack = Color(0xFF000000)
}

@Composable
internal fun ScanditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ScanditColors.Black,
            onPrimary = ScanditColors.White,
            secondary = ScanditColors.Cyan,
            onSecondary = ScanditColors.Black,
            surface = ScanditColors.White,
            onSurface = ScanditColors.Black,
            surfaceVariant = ScanditColors.LightGrey,
            onSurfaceVariant = ScanditColors.Black,
            background = ScanditColors.White,
            onBackground = ScanditColors.Black,
        ),
        content = content,
    )
}
