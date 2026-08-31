/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample.behaviours

import com.kmp.datacapture.barcode.ar.BarcodeArAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArCircleHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArCircleHighlightPreset
import com.kmp.datacapture.barcode.ar.BarcodeArHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArPopoverAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArPopoverAnnotationButton
import com.kmp.datacapture.barcode.ar.BarcodeArPopoverAnnotationListener
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.common.Color
import com.kmp.datacapture.core.ui.ScanditIcon
import com.kmp.datacapture.core.ui.ScanditIconShape
import com.kmp.datacapture.core.ui.ScanditIconType
import com.kmp.datacapture.core.ui.style.Brush

// Popovers mode: a circular icon highlight per barcode plus (for barcodes marked "Wrong") a
// popover with Accept/Reject buttons. Accepting/rejecting updates the highlight and resets the
// view so the provider is re-queried without re-showing a settled popover — mirrors native
// ScanPopoversBehaviour.
internal class ArPopoversBehaviour(
    private val resetView: () -> Unit,
) : ArScanBehaviour {

    private enum class BarcodeStatus { Accepted, Rejected, Wrong }

    private val highlights: MutableMap<String, BarcodeArCircleHighlight> = mutableMapOf()
    private val barcodeStatus: MutableMap<String, BarcodeStatus> = mutableMapOf()

    private val redColor = Color.fromRgba(0xD9, 0x21, 0x21)
    private val greenColor = Color.fromRgba(0x0D, 0x85, 0x3D)
    private val white = Color.fromRgba(0xFF, 0xFF, 0xFF)

    override fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit) {
        val data = barcode.data ?: return callback(null)
        if (data !in barcodeStatus) {
            barcodeStatus[data] = if (barcodeStatus.size % 2 == 0) BarcodeStatus.Wrong else BarcodeStatus.Accepted
        }
        val highlight = highlights.getOrPut(data) {
            BarcodeArCircleHighlight(barcode, BarcodeArCircleHighlightPreset.ICON)
        }
        updateHighlight(highlight, data)
        callback(highlight)
    }

    override fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit) {
        val data = barcode.data
        if (data == null || barcodeStatus[data] != BarcodeStatus.Wrong) return callback(null)

        val rejectIcon = ScanditIcon.builder()
            .withIcon(ScanditIconType.X_MARK)
            .withIconColor(white)
            .withBackgroundShape(ScanditIconShape.CIRCLE)
            .withBackgroundColor(redColor)
            .build()
        val acceptIcon = ScanditIcon.builder()
            .withIcon(ScanditIconType.CHECKMARK)
            .withIconColor(white)
            .withBackgroundShape(ScanditIconShape.CIRCLE)
            .withBackgroundColor(greenColor)
            .build()

        val annotation = BarcodeArPopoverAnnotation(
            barcode,
            listOf(
                BarcodeArPopoverAnnotationButton(rejectIcon, "Reject"),
                BarcodeArPopoverAnnotationButton(acceptIcon, "Accept"),
            ),
        )
        annotation.listener = object : BarcodeArPopoverAnnotationListener {
            override fun onPopoverButtonTapped(
                popover: BarcodeArPopoverAnnotation,
                button: BarcodeArPopoverAnnotationButton,
                buttonIndex: Int,
            ) {
                val barcodeData = popover.barcode.data ?: return
                barcodeStatus[barcodeData] = if (buttonIndex == 0) BarcodeStatus.Rejected else BarcodeStatus.Accepted
                highlights[barcodeData]?.let { updateHighlight(it, barcodeData) }
                // Reset the AR view to clear cached annotations and re-query providers, so
                // settled (accepted/rejected) barcodes don't show the popover again.
                resetView()
            }
        }

        callback(annotation)
    }

    private fun updateHighlight(highlight: BarcodeArCircleHighlight, data: String) {
        when (barcodeStatus[data]) {
            BarcodeStatus.Accepted -> {
                highlight.brush = Brush(greenColor, greenColor, 1f)
                highlight.icon = ScanditIcon.builder()
                    .withIcon(ScanditIconType.CHECKMARK)
                    .withIconColor(white)
                    .build()
                highlight.isPulsing = false
            }
            BarcodeStatus.Wrong -> {
                highlight.brush = Brush(redColor, redColor, 1f)
                highlight.icon = ScanditIcon.builder()
                    .withIcon(ScanditIconType.EXCLAMATION_MARK)
                    .withIconColor(white)
                    .build()
                highlight.isPulsing = true
            }
            BarcodeStatus.Rejected -> {
                highlight.brush = Brush(redColor, redColor, 1f)
                highlight.icon = ScanditIcon.builder()
                    .withIcon(ScanditIconType.X_MARK)
                    .withIconColor(white)
                    .build()
                highlight.isPulsing = false
            }
            null -> error("Barcode with no data")
        }
    }
}
