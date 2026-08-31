/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette lifted directly from the native RestockingSample's colors.xml so
// the result screen (Picklist / Inventory headers, picked checkmark) matches
// look-for-look.
internal object ScanditColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val LightGrey = Color(0xFFF8FAFC)
    val Black900 = Color(0xFF121619)
    val Grey500 = Color(0xFF8795A1)
    val Teal = Color(0xFF2EC1CE)
    val Red = Color(0xFFFA4446)
    val Amber = Color(0xFFFBC02C)
    val GreySlimSeparator = Color(0xFFDAE1E7)
    val GreySeparator = Color(0xFFF1F5F8)
}

@Composable
internal fun ScanditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ScanditColors.Black,
            onPrimary = ScanditColors.White,
            secondary = ScanditColors.Teal,
            onSecondary = ScanditColors.Black,
            surface = ScanditColors.White,
            onSurface = ScanditColors.Black900,
            surfaceVariant = ScanditColors.LightGrey,
            onSurfaceVariant = ScanditColors.Black900,
            background = ScanditColors.White,
            onBackground = ScanditColors.Black900,
        ),
        content = content,
    )
}
