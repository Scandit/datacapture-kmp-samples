/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

/**
 * One aggregated row of the MatrixScan Count result list. Mirrors the native
 * Android sample's `ScanDetails`, but under a unique name — `ScanDetails` /
 * `ScannedBarcode` risk colliding with an exported SDK Objective-C type once
 * this module is bundled into the iOS XCFramework (SDC-31092 postmortem).
 *
 * Keyed by barcode data alone (matching the native sample's `HashMap<String,
 * ScanDetails>`): a barcode is aggregated by its decoded data, and the last
 * observed symbology for that data wins.
 */
data class CountedBarcode(
    val data: String,
    val symbology: String,
    val quantity: Int,
)
