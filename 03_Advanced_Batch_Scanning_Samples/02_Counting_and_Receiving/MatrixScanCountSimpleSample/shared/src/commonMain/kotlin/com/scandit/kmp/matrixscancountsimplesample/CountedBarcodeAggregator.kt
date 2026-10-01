/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

/**
 * Pure, SDK-free aggregation of scanned barcodes into [CountedBarcode] rows,
 * keyed by barcode data — mirrors the native sample's
 * `BarcodeManager.getScanResults()`. Kept separate from
 * [MatrixScanCountScreenModel] so the aggregation rules are unit-testable
 * without any Scandit SDK objects.
 *
 * `BarcodeCountSession.recognizedBarcodes` is a *cumulative* list — one entry
 * per physical scan occurrence for the whole session, not deduplicated and
 * not a delta since the last callback — so aggregation always recomputes from
 * the full snapshot rather than incrementing counts across calls.
 */
internal object CountedBarcodeAggregator {

    /**
     * Builds the aggregated rows from every scanned occurrence in [scans]
     * (barcode data paired with its symbology name), in first-seen order.
     * The first-seen symbology for a given data sticks; repeats only bump
     * the quantity, matching the native sample's `BarcodeManager` semantics.
     */
    fun aggregate(scans: List<Pair<String, String>>): List<CountedBarcode> {
        val countsByData = LinkedHashMap<String, CountedBarcode>()
        for ((data, symbology) in scans) {
            val existing = countsByData[data]
            countsByData[data] = if (existing != null) {
                existing.copy(quantity = existing.quantity + 1)
            } else {
                CountedBarcode(data = data, symbology = symbology, quantity = 1)
            }
        }
        return countsByData.values.toList()
    }
}
