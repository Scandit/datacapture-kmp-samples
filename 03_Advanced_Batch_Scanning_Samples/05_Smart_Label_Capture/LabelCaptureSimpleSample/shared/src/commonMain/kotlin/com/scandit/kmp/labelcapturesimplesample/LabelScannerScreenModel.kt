/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.common.Color
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.LogoStyle
import com.kmp.datacapture.core.ui.style.Brush
import com.kmp.datacapture.label.capture.CapturedLabel
import com.kmp.datacapture.label.capture.LabelCapture
import com.kmp.datacapture.label.capture.LabelCaptureSettings
import com.kmp.datacapture.label.capture.LabelField
import com.kmp.datacapture.label.capture.labelCaptureSettings
import com.kmp.datacapture.label.definition.LabelDateComponentFormat
import com.kmp.datacapture.label.definition.LabelDateFormat
import com.kmp.datacapture.label.definition.LabelDateResult
import com.kmp.datacapture.label.ui.overlay.LabelCaptureBasicOverlay
import com.kmp.datacapture.label.ui.overlay.LabelCaptureBasicOverlayListener
import com.kmp.datacapture.label.ui.overlay.LabelCaptureValidationFlowListener
import com.kmp.datacapture.label.ui.overlay.LabelCaptureValidationFlowOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model. Owns every Scandit SDK object the label
// scanner needs, exposes the [state] StateFlow each platform observes, and
// accepts platform intents through [onEvent].
//
// Capture is driven by the validation-flow overlay (mirrors the native
// LabelCaptureSimpleSample): once a label is captured and validated, the
// SDK pauses the camera and disables the mode, then fires
// [LabelCaptureValidationFlowListener.onValidationFlowLabelCaptured]. We
// surface the fields as [LabelScannerUiState.Scanned]; dismissing the
// result re-arms capture.
class LabelScannerScreenModel {

    private val _state = MutableStateFlow<LabelScannerUiState>(LabelScannerUiState.Scanning)
    val state: StateFlow<LabelScannerUiState> = _state.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [state] directly via Compose's `collectAsState`; iOS lacks a
    // built-in StateFlow → SwiftUI bridge, so the host calls [observeState]
    // and stores the returned [FlowCancellable] for the view's lifetime.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeState(onEach: (LabelScannerUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    private val camera: Camera? =
        Camera.getDefaultCamera(LabelCapture.createRecommendedCameraSettings())?.also {
            dataCaptureContext.setFrameSource(it)
        }

    // The label to recognise: a simple retail item with a barcode, an expiry
    // date (month/day/year) and an optional total price. Built with the
    // Kotlin DSL added for KMP parity with the native SDK.
    private val settings: LabelCaptureSettings =
        labelCaptureSettings {
            label(LABEL_NAME) {
                customBarcode(FIELD_BARCODE) {
                    setSymbologies(
                        Symbology.EAN13_UPCA,
                        Symbology.GS1_DATABAR_EXPANDED,
                        Symbology.CODE128,
                    )
                }
                expiryDateText(FIELD_EXPIRY_DATE) {
                    setLabelDateFormat(
                        LabelDateFormat(
                            componentFormat = LabelDateComponentFormat.MDY,
                            acceptPartialDates = false,
                        ),
                    )
                }
                totalPriceText(FIELD_TOTAL_PRICE) {
                    isOptional(true)
                }
            }
        }

    private val labelCapture: LabelCapture =
        LabelCapture.forContext(dataCaptureContext, settings)

    // Held so [onEvent] can forward the host's resume/pause lifecycle to the
    // validation flow (Android-backed; no-op on iOS).
    private var validationFlowOverlay: LabelCaptureValidationFlowOverlay? = null

    // Transparent so the basic overlay doesn't draw a box over the whole
    // label — the validation-flow overlay owns the on-screen affordances.
    private val transparentBrush = Brush(
        fillColor = Color.fromRgba(0, 0, 0, 0),
        strokeColor = Color.fromRgba(0, 0, 0, 0),
        strokeWidth = 0f,
    )

    fun setupDataCaptureView(view: DataCaptureView): DataCaptureView {
        view.logoStyle = LogoStyle.MINIMAL

        val basicOverlay = LabelCaptureBasicOverlay.withLabelCapture(labelCapture).also { overlay ->
            overlay.listener = object : LabelCaptureBasicOverlayListener {
                override fun brushForLabel(
                    overlay: LabelCaptureBasicOverlay,
                    label: CapturedLabel,
                ): Brush = transparentBrush
            }
        }
        view.addOverlay(basicOverlay)

        // Android requires the view-bound factory (withLabelCapture() throws on
        // Android — the validation-flow overlay needs the DataCaptureView to host
        // its result UI). The nullable-view factory works on both platforms.
        val flowOverlay = LabelCaptureValidationFlowOverlay.withLabelCaptureForView(labelCapture, view).also { overlay ->
            overlay.listener = object : LabelCaptureValidationFlowListener {
                override fun onValidationFlowLabelCaptured(
                    overlay: LabelCaptureValidationFlowOverlay,
                    fields: List<LabelField>,
                ) {
                    // The SDK has already paused the camera and disabled the
                    // mode at this point; surface the result and wait for the
                    // user to dismiss it (see DISMISS_RESULT).
                    _state.value = LabelScannerUiState.Scanned(fields.toResults())
                }
            }
        }
        view.addOverlay(flowOverlay)
        validationFlowOverlay = flowOverlay
        return view
    }

    // True until the first STARTED. A just-created flow cannot be paused,
    // so starting the camera explicitly is always correct here — and needed:
    // on Android the first STARTED fires during composition, before the
    // overlay is attached to the view, so its onResume() alone cannot be
    // relied on to start the camera.
    private var isFirstStart = true

    fun onEvent(event: LabelScannerEvent) {
        when (event) {
            LabelScannerEvent.STARTED -> {
                // When the overlay owns the lifecycle (Android), its
                // onResume() restores the camera and re-enables the mode —
                // preserving a paused state across backgrounding. We only
                // drive the camera ourselves on the cold start (see below)
                // or on platforms where the overlay hooks are no-ops (iOS).
                validationFlowOverlay?.onResume()
                if (isFirstStart || !overlayDrivesCameraLifecycle) {
                    labelCapture.isEnabled = true
                    camera?.switchToDesiredState(FrameSourceState.ON)
                }
                isFirstStart = false
            }
            LabelScannerEvent.STOPPED -> {
                // Always stop the camera when leaving the foreground. Unlike
                // the STARTED side, forcing OFF here can't clobber a paused
                // state — the overlay restores its own state on resume — and
                // it's required for the window before the overlay is attached
                // (its onPause() is a no-op then, which would otherwise leave
                // the camera streaming in the background).
                validationFlowOverlay?.onPause()
                labelCapture.isEnabled = false
                camera?.switchToDesiredState(FrameSourceState.OFF)
            }
            LabelScannerEvent.DISMISS_RESULT -> {
                if (_state.value !is LabelScannerUiState.Scanning) {
                    _state.value = LabelScannerUiState.Scanning
                    labelCapture.isEnabled = true
                    camera?.switchToDesiredState(FrameSourceState.ON)
                }
            }
        }
    }

    private fun List<LabelField>.toResults(): List<LabelFieldResult> = mapNotNull { field ->
        val date = field.asDate()
        if (date != null) {
            LabelFieldResult(field.name, formatDate(date))
        } else {
            val data = field.barcode?.data ?: field.text
            if (!data.isNullOrBlank()) LabelFieldResult(field.name, data) else null
        }
    }

    private fun formatDate(date: LabelDateResult): String {
        val day = date.day
        val month = date.month
        val year = date.year
        return if (day != null && month != null && year != null) {
            "${day.toString().padStart(2, '0')} - ${month.toString().padStart(2, '0')} - $year"
        } else {
            "${date.dayString} - ${date.monthString} - ${date.yearString}"
        }
    }

    fun dispose() {
        labelCapture.isEnabled = false
        validationFlowOverlay?.onPause()
        validationFlowOverlay = null
        dataCaptureContext.removeMode(labelCapture)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    companion object {
        private const val LABEL_NAME = "Retail Item"
        private const val FIELD_BARCODE = "barcode"
        private const val FIELD_EXPIRY_DATE = "expiry date"
        private const val FIELD_TOTAL_PRICE = "Total Price"

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
