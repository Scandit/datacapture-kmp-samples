/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import kotlin.test.Test
import kotlin.test.assertEquals

class CountedBarcodeAggregatorTest {

    @Test
    fun sameDataScannedTwice_yieldsQuantityTwo() {
        val result = CountedBarcodeAggregator.aggregate(
            listOf("A" to "EAN13_UPCA", "A" to "EAN13_UPCA"),
        )

        assertEquals(1, result.size)
        assertEquals(2, result[0].quantity)
        assertEquals("A", result[0].data)
    }

    @Test
    fun distinctDatas_yieldDistinctRows() {
        val result = CountedBarcodeAggregator.aggregate(
            listOf("A" to "EAN13_UPCA", "B" to "CODE128"),
        )

        assertEquals(2, result.size)
        assertEquals(listOf("A", "B"), result.map { it.data })
        result.forEach { assertEquals(1, it.quantity) }
    }

    @Test
    fun emptyScans_yieldsEmptyList() {
        assertEquals(emptyList(), CountedBarcodeAggregator.aggregate(emptyList()))
    }

    @Test
    fun firstSeenSymbology_sticksForRepeatedData() {
        // Matches native BarcodeManager: first-seen symbology sticks; repeats only bump quantity.
        val result = CountedBarcodeAggregator.aggregate(
            listOf("A" to "CODE39", "A" to "CODE128"),
        )

        assertEquals(1, result.size)
        assertEquals("CODE39", result[0].symbology)
        assertEquals(2, result[0].quantity)
    }
}
