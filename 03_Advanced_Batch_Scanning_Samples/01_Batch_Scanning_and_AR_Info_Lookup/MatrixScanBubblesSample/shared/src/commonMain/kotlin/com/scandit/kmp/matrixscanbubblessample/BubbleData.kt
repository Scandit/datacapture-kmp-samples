/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanbubblessample

// Hardcoded "back-end" stock data shown inside a bubble. A real app would
// look this up from its inventory system keyed on the scanned code; the
// sample just returns a fixed shelf/back-room count so the focus stays on
// the AR-overlay wiring.
//
// Mirrors the native MatrixScanBubblesSample's `BubbleData` /
// `BubbleDataProvider`.
data class BubbleData(
    val shelfCount: Int,
    val backroomCount: Int,
) {
    companion object {
        // The native sample hardcodes (shelf 4, back room 8) for every code.
        fun forBarcode(@Suppress("UNUSED_PARAMETER") code: String): BubbleData =
            BubbleData(shelfCount = 4, backroomCount = 8)
    }
}
