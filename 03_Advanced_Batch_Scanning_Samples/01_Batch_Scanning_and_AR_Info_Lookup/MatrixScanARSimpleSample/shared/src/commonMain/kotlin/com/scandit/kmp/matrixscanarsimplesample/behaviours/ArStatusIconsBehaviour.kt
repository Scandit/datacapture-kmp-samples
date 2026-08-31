/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample.behaviours

import com.kmp.datacapture.barcode.ar.BarcodeArAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArRectangleHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArStatusIconAnnotation
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.common.Color
import com.kmp.datacapture.core.ui.ScanditIcon
import com.kmp.datacapture.core.ui.ScanditIconShape
import com.kmp.datacapture.core.ui.ScanditIconType

// StatusIcons mode: a plain rectangle highlight plus a status-icon annotation showing
// "Close to expiry" (yellow) or "Item expired" (red), alternating per newly-seen barcode —
// mirrors native ScanStatusIconsBehaviour.
internal class ArStatusIconsBehaviour : ArScanBehaviour {

    private enum class BarcodeStatus { CloseToExpiry, Expired }

    private val barcodeStatus: MutableMap<String, BarcodeStatus> = mutableMapOf()

    private val redColor = Color.fromRgba(0xD9, 0x21, 0x21)
    private val yellowColor = Color.fromRgba(0xFB, 0xC0, 0x2C)
    private val black = Color.fromRgba(0x00, 0x00, 0x00)
    private val white = Color.fromRgba(0xFF, 0xFF, 0xFF)

    override fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit) {
        val data = barcode.data ?: return callback(null)
        if (data !in barcodeStatus) {
            barcodeStatus[data] =
                if (barcodeStatus.size % 2 == 0) BarcodeStatus.CloseToExpiry else BarcodeStatus.Expired
        }
        callback(BarcodeArRectangleHighlight(barcode))
    }

    override fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit) {
        val data = barcode.data ?: return callback(null)
        val annotation = BarcodeArStatusIconAnnotation(barcode)
        when (barcodeStatus[data]) {
            BarcodeStatus.CloseToExpiry -> {
                annotation.text = "Close to expiry"
                annotation.icon = ScanditIcon.builder()
                    .withBackgroundShape(ScanditIconShape.CIRCLE)
                    .withBackgroundColor(yellowColor)
                    .withIcon(ScanditIconType.EXCLAMATION_MARK)
                    .withIconColor(black)
                    .build()
            }
            BarcodeStatus.Expired -> {
                annotation.text = "Item expired"
                annotation.icon = ScanditIcon.builder()
                    .withBackgroundShape(ScanditIconShape.CIRCLE)
                    .withBackgroundColor(redColor)
                    .withIcon(ScanditIconType.EXCLAMATION_MARK)
                    .withIconColor(white)
                    .build()
            }
            // No status assigned yet (annotation queried before the highlight): skip rather
            // than throw — the next frame's highlight pass will assign one.
            null -> return callback(null)
        }
        callback(annotation)
    }
}
