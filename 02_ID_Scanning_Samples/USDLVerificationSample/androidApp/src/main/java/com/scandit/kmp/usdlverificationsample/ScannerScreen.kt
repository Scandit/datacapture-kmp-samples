/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kmp.datacapture.core.ui.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.toAndroidView

// Scanner screen: full-screen camera preview under a thin "Scan" top bar.
// The Scaffold gives us the back affordance + title; `IdScannerHost` owns the
// actual SDK wiring (screen model, lifecycle observer, view embed).
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
                // Light Grey on Scandit Black instead of solid black — gives
                // a subtle, on-brand divider above the camera preview without
                // a heavy bar dominating the screen.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.LightGrey,
                    titleContentColor = ScanditColors.Black,
                    navigationIconContentColor = ScanditColors.Black,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            IdScannerHost()
        }
    }
}

@Composable
private fun IdScannerHost() {
    val context = LocalContext.current
    val screenModel = remember { IdScannerScreenModel() }
    val state by screenModel.state.collectAsState()

    val dataCaptureView = remember {
        screenModel.setupDataCaptureView(
            DataCaptureView(context, screenModel.dataCaptureContext),
        )
    }

    // Two concerns to handle here:
    //  1. Activity background/foreground (system pause/resume) — must
    //     stop the camera or we keep streaming with the app in background.
    //  2. Composition leave (user navigates back to home) — Activity stays
    //     RESUMED, so ON_PAUSE never fires; we still need to stop the
    //     camera explicitly in `onDispose`.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onEvent(IdScannerEvent.STARTED)
                Lifecycle.Event.ON_PAUSE -> screenModel.onEvent(IdScannerEvent.STOPPED)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Tear down every SDK resource the model owns — also turns the
            // camera off. STOPPED alone would just disable the mode and
            // leave it attached to the shared DataCaptureContext; over
            // multiple visits those modes would compound and slow scanning.
            screenModel.dispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The shared model produces the SDK view; the host is the only place
        // that knows how to embed an Android View in a Compose tree.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val nativeView: View = dataCaptureView.toAndroidView()
                (nativeView.parent as? ViewGroup)?.removeView(nativeView)
                nativeView
            },
        )

        val onDismiss = { screenModel.onEvent(IdScannerEvent.DISMISS_RESULT) }
        when (val current = state) {
            is IdScannerUiState.Result -> ResultSheet(result = current, onDismiss = onDismiss)
            is IdScannerUiState.Rejected -> ResultDialog(
                title = "Error Encountered",
                message = current.message,
                onDismiss = onDismiss,
            )
            IdScannerUiState.Scanning -> Unit
        }
    }
}

// Rich verification result, mirroring the native USDLVerificationSample's
// bottom sheet: captured image, per-check tiles, then the full field list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultSheet(result: IdScannerUiState.Result, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = result.title, style = MaterialTheme.typography.headlineSmall)

            result.image?.let { image ->
                Image(
                    bitmap = remember(image) { image.toBitmap().asImageBitmap() },
                    contentDescription = "Captured document image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            result.checks.forEach { check -> VerificationTile(check) }

            HorizontalDivider()

            result.fields.forEach { field -> FieldRow(field) }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Resume") }
        }
    }
}

@Composable
private fun VerificationTile(check: IdScannerUiState.VerificationCheck) {
    val color = if (check.passed) ScanditColors.Green else ScanditColors.Red
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Close,
            contentDescription = null,
            tint = color,
        )
        Text(text = check.label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FieldRow(field: IdScannerUiState.Field) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.labelMedium,
            color = ScanditColors.Grey,
        )
        Text(text = field.value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Resume") }
        },
        title = { Text(title) },
        text = { Text(message) },
    )
}
