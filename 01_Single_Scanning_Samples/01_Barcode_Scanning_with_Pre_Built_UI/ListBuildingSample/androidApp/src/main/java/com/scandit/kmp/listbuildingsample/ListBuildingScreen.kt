/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmp.datacapture.barcode.spark.SparkScanView
import com.kmp.datacapture.barcode.ui.toAndroidView

// List-building screen. The SparkScanView's pre-built UI floats its trigger
// button + mini preview over the full screen; the scanned-item list, count
// header, and Clear button render underneath it.
@Composable
internal fun ListBuildingScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val screenModel = remember { ListBuildingScreenModel() }
    val items by screenModel.items.collectAsState()
    val scannedCount by screenModel.scannedCount.collectAsState()

    // SparkScanView is built here (the host is the only place with a Context)
    // and wired to the model's shared context / mode / view settings.
    val sparkScanView = remember {
        SparkScanView(
            context,
            screenModel.dataCaptureContext,
            screenModel.sparkScan,
            screenModel.sparkScanViewSettings,
        ).also { it.feedbackDelegate = screenModel }
    }

    // Lifecycle: resume/pause the SparkScanView with the Activity, and dispose
    // the model (detaching the listener + removing the mode) when the screen
    // leaves the composition. ON_PAUSE doesn't fire on a back-navigation while
    // the Activity stays RESUMED, so onDispose handles that path.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sparkScanView.onResume()
                Lifecycle.Event.ON_PAUSE -> sparkScanView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        sparkScanView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sparkScanView.onPause()
            sparkScanView.feedbackDelegate = null
            screenModel.dispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScanditColors.White)) {
        // List content underneath the SparkScan pre-built UI. Pad for the
        // system bars so the count header clears the status bar and the Clear
        // button clears the navigation bar (the Activity draws edge-to-edge).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            ItemCountHeader(scannedCount)
            HorizontalDivider(color = ScanditColors.LightGrey)

            val listState = rememberLazyListState()
            // Scroll to the top whenever a different item lands at the front
            // (new barcode, or a re-scanned one moved up). Keyed on the front
            // item's identity, not its reference, so quantity/thumbnail bumps
            // on the same front row don't re-trigger the scroll.
            val frontKey = items.firstOrNull()?.let { it.barcodeData to it.symbology }
            LaunchedEffect(frontKey) {
                if (items.isNotEmpty()) listState.animateScrollToItem(0)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
            ) {
                // data+symbology is the dedup identity — same data scanned as two
                // symbologies is two rows, so the data alone is not a unique key.
                itemsIndexed(
                    items,
                    key = { _, item -> "${item.barcodeData}${item.symbology}" },
                ) { index, item ->
                    ResultRow(item)
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = ScanditColors.LightGrey)
                    }
                }
            }

            Button(
                onClick = { screenModel.clearList() },
                enabled = items.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScanditColors.Black,
                    contentColor = ScanditColors.White,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("CLEAR LIST")
            }
        }

        // SparkScan pre-built UI overlay. Its coordinator layout is transparent
        // except for the trigger button + mini preview, so it sits on top of
        // the list without hiding it. Inset it for the system bars too — the
        // Activity draws edge-to-edge, so an un-inset overlay would tuck the
        // floating mini-preview under the status bar (it's positioned relative
        // to this container's bounds).
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            factory = {
                val native: View = sparkScanView.toAndroidView()
                (native.parent as? ViewGroup)?.removeView(native)
                native
            },
        )
    }
}

@Composable
private fun ItemCountHeader(count: Int) {
    val label = if (count == 1) "$count item scanned" else "$count items scanned"
    Text(
        text = label,
        color = ScanditColors.Black,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(ScanditColors.White)
            .padding(16.dp),
    )
}

@Composable
private fun ResultRow(item: ListBuildingEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScanditColors.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbnailStack(item.images)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.barcodeData,
                color = ScanditColors.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.symbology,
                color = ScanditColors.Black.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
        }
        if (item.quantity > 1) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Qty: ${item.quantity}",
                color = ScanditColors.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

// A small stack of up to 3 thumbnails, each offset by a few dp so the
// underlying images peek out — mirrors the native StackedImageView.
@Composable
private fun ThumbnailStack(images: List<CroppedImage>) {
    val stackSize = 70.dp
    val peek = 4.dp
    val shown = images.takeLast(ListBuildingEntry.MAX_IMAGES)

    Box(
        modifier = Modifier
            .size(stackSize)
            .background(ScanditColors.LightGrey),
    ) {
        shown.forEachIndexed { i, image ->
            val frontOffset = peek * (shown.size - 1 - i)
            // Convert once per image, not on every recomposition.
            val imageBitmap = remember(image) { image.toBitmap().asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = frontOffset, y = frontOffset)
                    .size(stackSize - peek * (shown.size - 1))
                    .clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}
