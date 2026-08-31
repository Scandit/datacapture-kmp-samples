/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

/**
 * Pure, SDK-free model of the scanned-barcode list and its dedup/quantity
 * rules. Kept separate from [ListBuildingScreenModel] so the ordering and
 * dedup behaviour is unit-testable without any Scandit SDK objects.
 *
 * Newest-first ordering: a freshly-scanned barcode is prepended; a re-scan of
 * an existing barcode bumps its quantity, appends the new thumbnail (capped at
 * [ListBuildingEntry.MAX_IMAGES], drop-oldest) and moves it back to the front —
 * mirroring the native Android sample.
 */
internal class ScannedItemList {

    // Keyed by data + symbology so the same data scanned as two different
    // symbologies yields two rows (mirrors the native sample's equality check).
    // Insertion order is newest-first (front = most recently touched).
    private val itemsByKey = LinkedHashMap<String, ListBuildingEntry>()

    private fun keyOf(barcodeData: String, symbology: String) = "$barcodeData\u001f$symbology"

    /** Current list, newest first. */
    fun items(): List<ListBuildingEntry> = itemsByKey.values.toList()

    /** Total scans across all rows (sum of quantities). */
    fun scannedCount(): Int = itemsByKey.values.sumOf { it.quantity }

    /**
     * Records a scan of [barcodeData] / [symbology]. Returns the updated list.
     * A re-scan matches only when both data and symbology match.
     */
    fun record(barcodeData: String, symbology: String, image: CroppedImage?): List<ListBuildingEntry> {
        val key = keyOf(barcodeData, symbology)
        val existing = itemsByKey[key]
        val updated = if (existing != null) {
            itemsByKey.remove(key)
            existing.withAdditionalScan(image)
        } else {
            ListBuildingEntry.firstScan(barcodeData, symbology, image)
        }

        // Re-insert at the front: rebuild with the touched key first, then the
        // rest in their existing (newest-first) order.
        val reordered = buildMap(itemsByKey.size + 1) {
            put(key, updated)
            putAll(itemsByKey)
        }
        itemsByKey.clear()
        itemsByKey.putAll(reordered)

        return items()
    }

    fun clear() {
        itemsByKey.clear()
    }
}
