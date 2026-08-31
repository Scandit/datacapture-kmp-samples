/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodecapturesimplesample

import com.kmp.datacapture.barcode.capture.BarcodeCapture
import com.kmp.datacapture.barcode.capture.BarcodeCaptureListener
import com.kmp.datacapture.barcode.capture.BarcodeCaptureOverlay
import com.kmp.datacapture.barcode.capture.BarcodeCaptureSession
import com.kmp.datacapture.barcode.capture.BarcodeCaptureSettings
import com.kmp.datacapture.barcode.capture.BarcodeCaptureFeedback
import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.LogoStyle
import com.kmp.datacapture.core.ui.viewfinder.RectangularViewfinder
import com.kmp.datacapture.core.ui.viewfinder.RectangularViewfinderLineStyle
import com.kmp.datacapture.core.ui.viewfinder.RectangularViewfinderStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model. Owns every Scandit SDK object the scanner
// needs, exposes the [state] StateFlow each platform observes, and accepts
// platform intents through [onEvent].
class BarcodeScannerScreenModel : BarcodeCaptureListener {

    private val _state = MutableStateFlow<BarcodeScannerUiState>(BarcodeScannerUiState.Scanning)
    val state: StateFlow<BarcodeScannerUiState> = _state.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [state] directly via Compose's `collectAsState`; iOS lacks a
    // built-in StateFlow → SwiftUI bridge, so the host calls
    // [observeState] from `BarcodeScannerHostState.init` and stores the
    // returned [FlowCancellable] for the view's lifetime — no polling.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeState(onEach: (BarcodeScannerUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    private val camera: Camera? =
        Camera.getDefaultCamera(BarcodeCapture.createRecommendedCameraSettings())?.also {
            dataCaptureContext.setFrameSource(it)
        }

    private val settings: BarcodeCaptureSettings =
        BarcodeCaptureSettings.barcodeCaptureSettings().also {
            it.enableSymbology(Symbology.EAN13_UPCA, true)
            it.enableSymbology(Symbology.EAN8, true)
            it.enableSymbology(Symbology.UPCE, true)
            it.enableSymbology(Symbology.QR, true)
            it.enableSymbology(Symbology.DATA_MATRIX, true)
            it.enableSymbology(Symbology.CODE39, true)
            it.enableSymbology(Symbology.CODE128, true)
            it.enableSymbology(Symbology.INTERLEAVED_TWO_OF_FIVE, true)
        }

    // Public so the iOS host can wire the SDK's Swift bridge to it once.
    // The SDK internally bypasses K/N's per-callback ObjC retention via
    // a Swift-side listener implementation; this code path stays
    // platform-independent.
    val barcodeCapture: BarcodeCapture =
        BarcodeCapture.forContext(dataCaptureContext, settings).also {
            it.addListener(this)
            it.feedback = BarcodeCaptureFeedback.defaultFeedback()
        }

    fun setupDataCaptureView(view: DataCaptureView): DataCaptureView {
        view.logoStyle = LogoStyle.MINIMAL
        val overlay = BarcodeCaptureOverlay.withBarcodeCaptureForView(barcodeCapture, view)
        overlay.viewfinder = RectangularViewfinder.withStyleAndLineStyle(
            RectangularViewfinderStyle.SQUARE,
            RectangularViewfinderLineStyle.LIGHT,
        )
        view.addOverlay(overlay)
        return view
    }

    fun onEvent(event: BarcodeScannerEvent) {
        when (event) {
            BarcodeScannerEvent.STARTED -> {
                camera?.switchToDesiredState(FrameSourceState.ON)
                barcodeCapture.isEnabled = true
            }
            BarcodeScannerEvent.STOPPED -> {
                barcodeCapture.isEnabled = false
                camera?.switchToDesiredState(FrameSourceState.OFF)
            }
            BarcodeScannerEvent.DISMISS_RESULT -> {
                if (_state.value !is BarcodeScannerUiState.Scanning) {
                    _state.value = BarcodeScannerUiState.Scanning
                    barcodeCapture.isEnabled = true
                }
            }
        }
    }

    override fun onBarcodeScanned(
        barcodeCapture: BarcodeCapture,
        session: BarcodeCaptureSession,
        data: FrameData,
    ) {
        val barcode = session.newlyRecognizedBarcode ?: return
        barcodeCapture.isEnabled = false
        _state.value = BarcodeScannerUiState.Scanned(
            data = barcode.data ?: "",
            symbologyName = barcode.symbology.name,
        )
    }

    override fun onSessionUpdated(
        barcodeCapture: BarcodeCapture,
        session: BarcodeCaptureSession,
        data: FrameData,
    ) = Unit

    fun dispose() {
        barcodeCapture.isEnabled = false
        barcodeCapture.removeListener(this)
        dataCaptureContext.removeMode(barcodeCapture)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    companion object {
        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
