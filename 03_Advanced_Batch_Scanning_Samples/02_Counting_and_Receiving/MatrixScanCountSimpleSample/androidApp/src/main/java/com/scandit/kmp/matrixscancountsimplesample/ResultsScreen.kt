/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MatrixScan Count results screen — titled top bar with an up action,
 * header total, scrollable list of aggregated rows (barcode data,
 * symbology, quantity), a Done button whose behaviour depends on
 * [doneButtonStyle], and a Clear List button — mirrors the native sample's
 * `ResultsActivity` (title "Scan Results", up navigation, Done + Clear List
 * both on Resume and Done paths):
 *  - [DoneButtonStyle.RESUME]: navigates back, keeping the session.
 *  - [DoneButtonStyle.NEW_SCAN]: resets the session, then navigates back to
 *    a fresh Scanner.
 *  - Clear List: resets the session (same as New-Scan reset) and navigates
 *    back to the Scanner, regardless of [doneButtonStyle].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultsScreen(
    screenModel: MatrixScanCountScreenModel,
    doneButtonStyle: DoneButtonStyle,
    onDone: () -> Unit,
) {
    val items by screenModel.items.collectAsState()
    val totalCount by screenModel.totalCount.collectAsState()

    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_results)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.scan_results),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.ChromeBlack,
                    titleContentColor = ScanditColors.White,
                    navigationIconContentColor = ScanditColors.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScanditColors.White),
        ) {
            Text(
                text = stringResource(R.string.results_amount, totalCount),
                color = ScanditColors.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            HorizontalDivider(color = ScanditColors.LightGrey)

            // Native ResultsActivity splits results into two sections: items
            // scanned more than once ("Non-unique item N", with a quantity
            // badge) followed by items scanned exactly once ("Item N", no
            // badge). Each is numbered by its position within its section.
            val nonUniqueItems = items.filter { it.quantity > 1 }
            val uniqueItems = items.filter { it.quantity == 1 }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(nonUniqueItems, key = { _, it -> it.data }) { index, item ->
                    ResultRow(
                        title = stringResource(R.string.multiple_quantity_product_description, index + 1),
                        subtitle = stringResource(R.string.result_item, item.symbology, item.data),
                        quantity = item.quantity,
                    )
                    HorizontalDivider(color = ScanditColors.LightGrey)
                }
                itemsIndexed(uniqueItems, key = { _, it -> it.data }) { index, item ->
                    ResultRow(
                        title = stringResource(R.string.product_description, index + 1),
                        subtitle = stringResource(R.string.result_item, item.symbology, item.data),
                        quantity = null,
                    )
                    HorizontalDivider(color = ScanditColors.LightGrey)
                }
            }

            val doneTextRes = when (doneButtonStyle) {
                DoneButtonStyle.RESUME -> R.string.resume_scanning
                DoneButtonStyle.NEW_SCAN -> R.string.start_new_scanning
            }
            // Filled black primary button (native bg_rectangle_filled).
            Button(
                onClick = {
                    if (doneButtonStyle == DoneButtonStyle.NEW_SCAN) {
                        screenModel.resetSession()
                    }
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScanditColors.Black,
                    contentColor = ScanditColors.White,
                ),
                shape = RectangleShape,
            ) {
                Text(stringResource(doneTextRes).uppercase(), fontWeight = FontWeight.Bold)
            }

            // Outlined "Clear List" button (native bg_rectangle: white fill,
            // 2dp black border).
            Button(
                onClick = {
                    screenModel.resetSession()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScanditColors.White,
                    contentColor = ScanditColors.Black,
                ),
                border = BorderStroke(2.dp, ScanditColors.Black),
                shape = RectangleShape,
            ) {
                Text(stringResource(R.string.clear_list).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, quantity: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Placeholder product thumbnail — native shows a 70dp grey square.
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(ScanditColors.DarkGrey),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 24.dp, end = 10.dp)) {
            Text(
                text = title,
                color = ScanditColors.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = ScanditColors.SubtitleGrey,
                fontSize = 12.sp,
            )
        }
        if (quantity != null) {
            Text(
                text = stringResource(R.string.quantity, quantity),
                color = ScanditColors.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}
