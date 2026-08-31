/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodeselectionsimplesample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Scandit brand palette (see Scandit_Brand_Colors_2025).
//
// Primary, primary-neutral, and one secondary swatch are exposed; the rest
// of the secondary palette can be added if/when a screen needs it.
internal object ScanditColors {
    val Black = Color(0xFF141F2E)
    val White = Color(0xFFFFFFFF)
    val Cyan = Color(0xFF2EC1CE)
    val LightGrey = Color(0xFFF1F5F8)
    val Blue = Color(0xFF0252B5)
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
