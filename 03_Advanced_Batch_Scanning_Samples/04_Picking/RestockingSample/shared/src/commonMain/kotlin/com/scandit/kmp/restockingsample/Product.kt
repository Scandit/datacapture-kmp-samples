/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

// A simple product representation for demo purposes. Mirrors the native
// RestockingSample's `Product`: an [identifier] unique per product, a target
// [quantityToPick], and the set of barcode contents that resolve to it
// (multiple barcodes can point to the same product).
data class Product(
    val identifier: String,
    val quantityToPick: Int,
    val barcodeData: List<String>,
)
