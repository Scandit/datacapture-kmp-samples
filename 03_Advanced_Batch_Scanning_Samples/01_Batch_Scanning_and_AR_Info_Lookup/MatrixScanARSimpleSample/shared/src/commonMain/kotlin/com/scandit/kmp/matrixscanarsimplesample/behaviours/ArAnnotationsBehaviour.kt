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
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotationBodyComponent
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotationFooter
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotationHeader
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotationListener
import com.kmp.datacapture.barcode.ar.BarcodeArInfoAnnotationWidthPreset
import com.kmp.datacapture.barcode.ar.BarcodeArResponsiveAnnotation
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.common.Color
import com.kmp.datacapture.core.common.TextAlignment
import com.kmp.datacapture.core.ui.ScanditIcon
import com.kmp.datacapture.core.ui.ScanditIconShape
import com.kmp.datacapture.core.ui.ScanditIconType
import com.kmp.datacapture.core.ui.style.Brush

// Annotations mode: a plain dot highlight plus a [BarcodeArResponsiveAnnotation] that swaps
// between a small far-away info card and a large close-up card with header/footer/body and a
// tap listener that toggles the header color — mirrors native ScanAnnotationsBehaviour.
internal class ArAnnotationsBehaviour : ArScanBehaviour {

    private val highlightColor = Color.fromRgba(0x00, 0xFF, 0xFF)
    private val blueColor = Color.fromRgba(0x00, 0xFF, 0xFF)
    private val yellowColor = Color.fromRgba(0xF0, 0xBD, 0x30)
    private val closeUpAnnotationColor: MutableMap<String, Color> = mutableMapOf()

    override fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit) {
        val highlight = BarcodeArCircleHighlight(barcode, BarcodeArCircleHighlightPreset.DOT)
        highlight.brush = Brush(highlightColor, highlightColor, 1f)
        callback(highlight)
    }

    override fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit) {
        val annotationIcon = ScanditIcon.builder()
            .withBackgroundShape(ScanditIconShape.CIRCLE)
            .withBackgroundStrokeColor(Color.fromRgba(0, 0, 0))
            .withIcon(ScanditIconType.CHECKMARK)
            .withIconColor(Color.fromRgba(0, 0, 0))
            .build()

        val faraway = BarcodeArInfoAnnotation(barcode).apply {
            width = BarcodeArInfoAnnotationWidthPreset.MEDIUM
            body = listOf(BarcodeArInfoAnnotationBodyComponent().apply { text = "Body text" })
        }

        val closeup = BarcodeArInfoAnnotation(barcode).apply {
            width = BarcodeArInfoAnnotationWidthPreset.LARGE
            header = BarcodeArInfoAnnotationHeader().apply {
                text = "Header"
                icon = annotationIcon
            }
            footer = BarcodeArInfoAnnotationFooter().apply { text = "Tap to change color" }
            isEntireAnnotationTappable = true
            body = listOf(
                BarcodeArInfoAnnotationBodyComponent().apply {
                    text = "This is text in a large container.\nIt can have multiple lines"
                    textAlignment = TextAlignment.LEFT
                },
                BarcodeArInfoAnnotationBodyComponent().apply {
                    text = "Point"
                    textAlignment = TextAlignment.LEFT
                    leftIcon = annotationIcon
                },
                BarcodeArInfoAnnotationBodyComponent().apply {
                    text = "Point"
                    textAlignment = TextAlignment.LEFT
                    leftIcon = annotationIcon
                },
            )
            listener = object : BarcodeArInfoAnnotationListener {
                override fun onInfoAnnotationTapped(annotation: BarcodeArInfoAnnotation) {
                    val data = annotation.barcode.data ?: return
                    toggleHeaderColor(data)
                    updateAnnotation(annotation, data)
                }
            }
        }

        barcode.data?.let { updateAnnotation(closeup, it) }

        callback(BarcodeArResponsiveAnnotation(barcode, closeUpAnnotation = closeup, farAwayAnnotation = faraway))
    }

    private fun toggleHeaderColor(data: String) {
        closeUpAnnotationColor[data] = when (closeUpAnnotationColor[data]) {
            blueColor -> yellowColor
            yellowColor -> blueColor
            else -> blueColor
        }
    }

    private fun updateAnnotation(annotation: BarcodeArInfoAnnotation, data: String) {
        val color = closeUpAnnotationColor.getOrPut(data) { blueColor }
        annotation.header?.backgroundColor = color
    }
}
