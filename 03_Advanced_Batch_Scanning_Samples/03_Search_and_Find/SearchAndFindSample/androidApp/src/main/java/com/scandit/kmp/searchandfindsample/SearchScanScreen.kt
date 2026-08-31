/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kmp.datacapture.barcode.spark.SparkScanViewUiListener
import com.kmp.datacapture.barcode.ui.toAndroidView

// Native-parity colors (from the native sample's values/colors.xml +
// clear_list_text_color selector).
private val ToolbarDark = Color(0xFF212121)
private val CountText = Color(0xFF3D4852)
private val ClearEnabled = Color(0xFFFA4446)
private val ClearDisabled = Color(0xFFB8C2CC)

// Search phase. Mirrors the native sample: a dark "Search & Find" app bar, a
// white sub-row with the scanned count + CLEAR LIST, then the list of scanned
// barcodes with the SparkScanView overlaid. Navigation to the Find phase rides
// the SparkScanView's built-in Find button (no separate button), matching native.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScanScreen(
    screenModel: SearchAndFindScreenModel,
    scannedItems: List<SearchScannedBarcode>,
    onFind: () -> Unit,
    onExit: () -> Unit,
) {
    val scannedCount = scannedItems.size
    val context = LocalContext.current
    BackHandler(onBack = onExit)

    // SparkScanView is constructed here (its ctor needs an Android Context),
    // shows the built-in Find button, and routes that button's tap to onFind()
    // via the UI listener. Registered back with the model for lifecycle calls.
    val sparkScanView = remember {
        SparkScanView(
            context,
            screenModel.dataCaptureContext,
            screenModel.sparkScan,
            screenModel.sparkScanViewSettings,
        ).also { view ->
            view.barcodeFindButtonVisible = true
            view.uiListener = object : SparkScanViewUiListener {
                override fun onBarcodeFindButtonTap(view: SparkScanView) = onFind()
            }
            screenModel.registerSparkScanView(
                object : SparkScanViewHandle {
                    override fun onResume() = view.onResume()
                    override fun onPause() = view.onPause()
                },
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> screenModel.onSearchResumed()
                Lifecycle.Event.ON_PAUSE -> screenModel.onSearchPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            screenModel.unregisterSparkScanView()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Find") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ToolbarDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // White header row: scanned count + CLEAR LIST (greys when empty).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$scannedCount items scanned",
                    color = CountText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                )
                TextButton(
                    onClick = { screenModel.clearList() },
                    enabled = scannedCount > 0,
                ) {
                    Text(
                        text = "CLEAR LIST",
                        color = if (scannedCount > 0) ClearEnabled else ClearDisabled,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // The scanned-barcode list fills the body (mirrors the native
            // RecyclerView); the SparkScanView is overlaid on top so its floating
            // trigger / mini-preview / Find button stay reachable.
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(scannedItems) { item ->
                        ScannedItemRow(item)
                        HorizontalDivider()
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        val native: View = sparkScanView.toAndroidView()
                        (native.parent as? ViewGroup)?.removeView(native)
                        native
                    },
                )
            }
        }
    }
}

// One row of the scanned-barcode list, styled after the ListBuildingSample
// row: a cropped barcode thumbnail, the barcode value in bold over its
// readable symbology name. No quantity badge — every scan is its own row.
@Composable
private fun ScannedItemRow(item: SearchScannedBarcode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScanditColors.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(item.images.firstOrNull())
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.data,
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
    }
}

// The cropped barcode thumbnail; a grey placeholder box when the frame carried
// no image. Mirrors the ListBuildingSample thumbnail (single image, no stack).
@Composable
private fun Thumbnail(image: CroppedImage?) {
    val size = 70.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(ScanditColors.LightGrey),
    ) {
        if (image != null) {
            val imageBitmap = remember(image) { image.toBitmap().asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}
