/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample.behaviours

import com.kmp.datacapture.barcode.ar.BarcodeAr
import com.kmp.datacapture.barcode.ar.BarcodeArAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArRectangleHighlight
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.common.Color
import com.kmp.datacapture.core.ui.ScanditIcon
import com.kmp.datacapture.core.ui.ScanditIconType
import com.kmp.datacapture.core.ui.style.Brush

// Highlights mode: a tappable rectangular highlight per barcode. Tapping toggles a
// "selected" state that swaps the highlight's brush + adds a checkmark icon — mirrors
// native ScanHighlightsBehaviour.
internal class ArHighlightsBehaviour : ArScanBehaviour {

    private val highlights: MutableMap<String, BarcodeArRectangleHighlight> = mutableMapOf()
    private val selectedBarcodes: MutableSet<String> = mutableSetOf()

    private val unselectedFill = Color.fromRgba(0x00, 0xFF, 0xFF, 0x66)
    private val unselectedStroke = Color.fromRgba(0x00, 0xFF, 0xFF, 0xFF)
    private val selectedFill = Color.fromRgba(0x00, 0x00, 0xFF, 0x66)
    private val selectedStroke = Color.fromRgba(0x00, 0x00, 0xFF, 0xFF)

    private val strokeWidth = 1f

    override fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit) {
        val data = barcode.data ?: return callback(null)
        val highlight = highlights.getOrPut(data) { BarcodeArRectangleHighlight(barcode) }
        updateHighlight(highlight, data)
        callback(highlight)
    }

    override fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit) {
        // Not needed in Highlights mode.
        callback(null)
    }

    override fun onHighlightForBarcodeTapped(barcodeAr: BarcodeAr, barcode: Barcode, highlight: BarcodeArHighlight) {
        val data = barcode.data ?: return
        if (!selectedBarcodes.remove(data)) selectedBarcodes.add(data)
        (highlights[data])?.let { updateHighlight(it, data) }
    }

    private fun updateHighlight(highlight: BarcodeArRectangleHighlight, data: String) {
        if (data in selectedBarcodes) {
            highlight.brush = Brush(selectedFill, selectedStroke, strokeWidth)
            highlight.icon = ScanditIcon.builder()
                .withIcon(ScanditIconType.CHECKMARK)
                .withIconColor(Color.fromRgba(0xFF, 0xFF, 0xFF))
                .build()
        } else {
            highlight.brush = Brush(unselectedFill, unselectedStroke, strokeWidth)
            highlight.icon = null
        }
    }
}
