/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Static breakdown shown after the BarcodePickView's built-in finish button is
// tapped: a Picklist section (everything picked) followed by an Inventory
// section (scanned but not picked). Mirrors the native sample's
// ProductListFragment / item_header.xml / item_result.xml, down to the
// "Result List" action bar, the grey product-image placeholder, the
// teal/amber status icons and the Continue scanning / Finish button pair.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultScreen(
    items: List<ListItem>,
    onContinueScanning: () -> Unit,
    onFinishPickup: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                // The native up arrow returns to the picking session, same as
                // "Continue scanning".
                title = { Text("Result List", color = ScanditColors.White) },
                navigationIcon = {
                    IconButton(onClick = onContinueScanning) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScanditColors.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScanditColors.Black,
                    titleContentColor = ScanditColors.White,
                    navigationIconContentColor = ScanditColors.White,
                ),
            )
        },
        containerColor = ScanditColors.White,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    when (item) {
                        is RestockingHeader -> SectionHeaderRow(item)
                        is RestockingProductRow -> ProductRowItem(item)
                    }
                }
            }
            BottomButtons(onContinueScanning = onContinueScanning, onFinishPickup = onFinishPickup)
        }
    }
}

@Composable
private fun SectionHeaderRow(header: RestockingHeader) {
    val title = if (header.isPickList) "Picklist" else "Inventory (${header.count})"
    Column {
        Text(
            text = title,
            color = ScanditColors.Black900,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        // Header separator: 2dp grey_slim (matches native SeparatorDecorator).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ScanditColors.GreySlimSeparator),
        )
    }
}

@Composable
private fun ProductRowItem(product: RestockingProductRow) {
    val isUnknown = product.quantityToPick == 0 && product.picked
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Grey product-image placeholder (native item_result.xml imageView).
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ScanditColors.Grey500),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.identifier,
                    color = ScanditColors.Black900,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "GTIN: ${product.barcodeData}",
                    color = ScanditColors.Grey500,
                    fontSize = 14.sp,
                )
                if (isUnknown) {
                    Text(
                        text = "Picked item not in pick list",
                        color = ScanditColors.Red,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            StatusIcon(isUnknown = isUnknown, picked = product.picked)
        }
        // Item separator: 4dp grey (matches native SeparatorDecorator).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ScanditColors.GreySeparator),
        )
    }
}

// Right-aligned status icon, using the same vector drawables as the native
// sample: the teal ic_green_check disc for correctly picked items, the amber
// ic_warning disc for picked items not on the pick list, nothing otherwise.
@Composable
private fun StatusIcon(isUnknown: Boolean, picked: Boolean) {
    val drawable = when {
        isUnknown -> R.drawable.ic_warning
        picked -> R.drawable.ic_green_check
        else -> return
    }
    Image(
        painter = painterResource(id = drawable),
        contentDescription = if (isUnknown) "Not in pick list" else "Picked",
        modifier = Modifier.size(28.dp),
    )
}

@Composable
private fun BottomButtons(onContinueScanning: () -> Unit, onFinishPickup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(ScanditColors.White).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onContinueScanning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ScanditColors.Black,
                contentColor = ScanditColors.White,
            ),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(text = "Continue scanning", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        OutlinedButton(
            onClick = onFinishPickup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ScanditColors.White,
                contentColor = ScanditColors.Black,
            ),
            border = BorderStroke(1.dp, ScanditColors.Black900),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(text = "Finish", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
