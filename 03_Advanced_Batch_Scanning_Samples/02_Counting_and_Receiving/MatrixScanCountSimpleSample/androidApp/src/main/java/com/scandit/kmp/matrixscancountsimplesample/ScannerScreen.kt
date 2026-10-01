/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kmp.datacapture.barcode.compose.BarcodeCountView

/**
 * The MatrixScan Count scanning screen. A black "MatrixScan Count" top bar
 * (mirrors the native sample's dark ActionBar) over the SDK's pre-built
 * `BarcodeCountView`, bound to the screen model's own long-lived
 * [MatrixScanCountScreenModel.barcodeCount] instance so aggregated results
 * survive navigating to Results and back.
 *
 * List button -> [onListTap] (Results in Resume mode). Exit button ->
 * [onExitTap] (Results in New-Scan mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerScreen(
    screenModel: MatrixScanCountScreenModel,
    onListTap: () -> Unit,
    onExitTap: () -> Unit,
) {
    Scaffold(
        containerColor = ScanditColors.ChromeBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.ChromeBlack,
                    titleContentColor = ScanditColors.White,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BarcodeCountView(
                settings = screenModel.settings,
                modifier = Modifier.fillMaxSize(),
                showListButton = true,
                showExitButton = true,
                onExitTap = onExitTap,
                onListTap = onListTap,
                context = screenModel.dataCaptureContext,
                barcodeCount = screenModel.barcodeCount,
            )
        }
    }
}
