/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Landing screen: Scandit banner (wordmark + "MatrixScan AR"), followed by the mode-picker menu —
// a 2x2 card grid matching the native sample's MenuFragment layout. Each card shows an icon, bold
// title, and subtitle for one [ArMode]. Selecting a card gates on the camera permission before
// navigating into the scanner pre-configured for that mode.
@Composable
internal fun MenuScreen(onSelectMode: (ArMode) -> Unit) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<ArMode?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val mode = pendingMode
            pendingMode = null
            if (granted && mode != null) {
                onSelectMode(mode)
            } else {
                permissionDenied = true
            }
        },
    )

    fun requestMode(mode: ArMode) {
        permissionDenied = false
        val granted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onSelectMode(mode)
        } else {
            pendingMode = mode
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 350.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Image(
                    painter = painterResource(R.drawable.banner),
                    contentDescription = "Scandit MatrixScan AR",
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .size(width = 238.dp, height = 64.dp),
                )
                if (permissionDenied) {
                    Text(
                        text = "Camera permission is required to scan barcodes.",
                        color = ScanditColors.Black,
                    )
                }
                Text(
                    text = "Try These Features",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = ScanditColors.Black,
                )
            }
        }
        items(ArMode.entries) { mode ->
            ModeCard(mode = mode, onClick = { requestMode(mode) })
        }
    }
}

@Composable
private fun ModeCard(mode: ArMode, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(ScanditColors.Black)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Image(
            painter = painterResource(id = mode.iconRes()),
            contentDescription = null,
        )
        Text(
            text = mode.title(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = mode.subtitle(),
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun ArMode.iconRes(): Int = when (this) {
    ArMode.Highlights -> R.drawable.ic_highlights
    ArMode.Annotations -> R.drawable.ic_annotations
    ArMode.Popovers -> R.drawable.ic_popovers
    ArMode.StatusIcons -> R.drawable.ic_status_icons
}

private fun ArMode.title(): String = when (this) {
    ArMode.Highlights -> "Highlights"
    ArMode.Annotations -> "Info Annotations"
    ArMode.Popovers -> "Popovers"
    ArMode.StatusIcons -> "Status Icons"
}

private fun ArMode.subtitle(): String = when (this) {
    ArMode.Highlights -> "Visualize scanned codes"
    ArMode.Annotations -> "Show additional information"
    ArMode.Popovers -> "Take action on scanned codes"
    ArMode.StatusIcons -> "Provide short information"
}
