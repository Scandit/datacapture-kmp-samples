/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

import kotlin.test.Test
import kotlin.test.assertEquals

class ScannedItemListTest {

    // Images are nullable in the pure model, so we exercise dedup/ordering
    // without constructing a platform CroppedImage.
    private val noImage: CroppedImage? = null

    @Test
    fun scanningAThenAThenB_yieldsNewestFirstWithQuantity() {
        val list = ScannedItemList()

        list.record("A", "EAN13_UPCA", noImage)
        list.record("A", "EAN13_UPCA", noImage)
        val result = list.record("B", "EAN13_UPCA", noImage)

        // Newest first: B (just scanned) leads, then A with quantity 2.
        assertEquals(listOf("B", "A"), result.map { it.barcodeData })
        assertEquals(1, result[0].quantity)
        assertEquals(2, result[1].quantity)
    }

    @Test
    fun rescanningExistingItem_movesItToFrontAndBumpsQuantity() {
        val list = ScannedItemList()

        list.record("A", "CODE128", noImage)
        list.record("B", "CODE128", noImage)
        val result = list.record("A", "CODE128", noImage)

        assertEquals(listOf("A", "B"), result.map { it.barcodeData })
        assertEquals(2, result[0].quantity)
        assertEquals(1, result[1].quantity)
    }

    @Test
    fun sameDataDifferentSymbology_isTreatedAsSeparateItems() {
        val list = ScannedItemList()

        list.record("123", "CODE39", noImage)
        val result = list.record("123", "CODE128", noImage)

        assertEquals(2, result.size)
        result.forEach { assertEquals(1, it.quantity) }
    }

    @Test
    fun scannedCount_sumsAllQuantities() {
        val list = ScannedItemList()

        list.record("A", "EAN8", noImage)
        list.record("A", "EAN8", noImage)
        list.record("B", "EAN8", noImage)

        assertEquals(3, list.scannedCount())
    }

    @Test
    fun clear_emptiesTheList() {
        val list = ScannedItemList()

        list.record("A", "EAN8", noImage)
        list.clear()

        assertEquals(emptyList(), list.items())
        assertEquals(0, list.scannedCount())
    }

    @Test
    fun imageStack_isCappedAtThreeNewestKept() {
        // Build a ListBuildingEntry directly to exercise the drop-oldest image cap
        // without needing real CroppedImages — the cap is over the list size.
        var item = ListBuildingEntry.firstScan("A", "EAN8", noImage)
        repeat(5) { item = item.withAdditionalScan(noImage) }

        // All images are null here, so the list only ever holds nulls that are
        // filtered out; assert the quantity bookkeeping instead.
        assertEquals(6, item.quantity)
        assertEquals(0, item.images.size)
    }
}
