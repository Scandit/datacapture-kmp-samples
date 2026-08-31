/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample.behaviours

import com.kmp.datacapture.barcode.ar.BarcodeAr
import com.kmp.datacapture.barcode.ar.BarcodeArAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArHighlight
import com.kmp.datacapture.barcode.data.Barcode

// Named `ArScanBehaviour` (not `ScanBehaviour`) and its callback types are plain lambdas
// rather than SDK Callback interfaces (KMP's BarcodeArHighlightProvider /
// BarcodeArAnnotationProvider use `(T?) -> Unit` lambdas, unlike native's Callback.onData).
//
// Mirrors the four native ScanXyzBehaviour classes: each demo mode's highlight + annotation +
// tap-handling logic lives in its own behaviour so [MatrixScanARScreenModel] only dispatches to
// whichever one is selected.
internal interface ArScanBehaviour {
    fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit)

    fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit)

    fun onHighlightForBarcodeTapped(barcodeAr: BarcodeAr, barcode: Barcode, highlight: BarcodeArHighlight) {
        // Default no-op — only ScanHighlightsBehaviour needs tap handling.
    }
}
