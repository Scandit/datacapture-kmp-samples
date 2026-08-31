/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanarsimplesample

import com.kmp.datacapture.barcode.ar.BarcodeAr
import com.kmp.datacapture.barcode.ar.BarcodeArAnnotation
import com.kmp.datacapture.barcode.ar.BarcodeArAnnotationProvider
import com.kmp.datacapture.barcode.ar.BarcodeArHighlight
import com.kmp.datacapture.barcode.ar.BarcodeArHighlightProvider
import com.kmp.datacapture.barcode.ar.BarcodeArSettings
import com.kmp.datacapture.barcode.ar.BarcodeArView
import com.kmp.datacapture.barcode.ar.BarcodeArViewSettings
import com.kmp.datacapture.barcode.ar.BarcodeArViewUiListener
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.source.VideoResolution
import com.scandit.kmp.matrixscanarsimplesample.behaviours.ArAnnotationsBehaviour
import com.scandit.kmp.matrixscanarsimplesample.behaviours.ArHighlightsBehaviour
import com.scandit.kmp.matrixscanarsimplesample.behaviours.ArPopoversBehaviour
import com.scandit.kmp.matrixscanarsimplesample.behaviours.ArScanBehaviour
import com.scandit.kmp.matrixscanarsimplesample.behaviours.ArStatusIconsBehaviour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model for MatrixScan AR Simple. Owns every Scandit SDK object the AR
// view needs, exposes the [mode] StateFlow each platform observes to drive the mode-switch UI,
// and dispatches highlight/annotation/tap callbacks to whichever [ArScanBehaviour] the current
// mode selects.
//
// The shared layer owns all SDK wiring; the platform hosts only embed the platform-native
// BarcodeArView (a `android.view.View` / `UIView` the KMP wrapper constructs internally) and
// forward lifecycle events.
class MatrixScanARScreenModel : BarcodeArHighlightProvider, BarcodeArAnnotationProvider, BarcodeArViewUiListener {

    private val _mode = MutableStateFlow(ArMode.Highlights)
    val mode: StateFlow<ArMode> = _mode.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers collect [mode]
    // directly via Compose's `collectAsState`; iOS lacks a built-in StateFlow -> SwiftUI bridge,
    // so the host calls [observeMode] and stores the returned [FlowCancellable] for the view's
    // lifetime.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeMode(onEach: (ArMode) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            mode.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = DataCaptureContext.initialize(LICENSE_KEY)

    // The barcode ar process is configured through settings which are then applied to the
    // BarcodeAr instance. For the purpose of this sample we enable a generous set of
    // symbologies; in a real app enable only what you need, as every extra symbology costs
    // processing time.
    private val settings: BarcodeArSettings =
        BarcodeArSettings.barcodeArSettings().also {
            it.enableSymbology(Symbology.EAN13_UPCA, true)
            it.enableSymbology(Symbology.EAN8, true)
            it.enableSymbology(Symbology.UPCE, true)
            it.enableSymbology(Symbology.CODE39, true)
            it.enableSymbology(Symbology.CODE128, true)
            it.enableSymbology(Symbology.QR, true)
            it.enableSymbology(Symbology.DATA_MATRIX, true)
        }

    val barcodeAr: BarcodeAr = BarcodeAr.forContext(dataCaptureContext, settings)

    private var behaviour: ArScanBehaviour = behaviourFor(ArMode.Highlights)

    private var view: BarcodeArView? = null

    /** Builds the [BarcodeArViewSettings] the host should pass into the platform view ctor. */
    fun buildViewSettings(): BarcodeArViewSettings = BarcodeArViewSettings.barcodeArViewSettings()

    /**
     * Returns the camera settings recommended for BarcodeAr scanning, at UHD4K — mirrors the
     * native sample's `BarcodeAr.createRecommendedCameraSettings().apply { preferredResolution =
     * UHD4K }`. KMP's [BarcodeArView] constructs its own camera internally (unlike native
     * Android, which threads camera settings through the view ctor), so this is exposed for
     * documentation/reference; the KMP view already defaults to the recommended settings.
     */
    fun recommendedCameraSettings() = BarcodeAr.createRecommendedCameraSettings().apply {
        preferredResolution = VideoResolution.UHD4K
    }

    /**
     * Register an already-constructed [BarcodeArView] (each platform builds it differently),
     * wire providers + ui listener, and start scanning.
     */
    fun registerView(view: BarcodeArView): BarcodeArView {
        this.view = view
        view.highlightProvider = this
        view.annotationProvider = this
        view.uiListener = this
        view.start()
        return view
    }

    fun setMode(mode: ArMode) {
        _mode.value = mode
        behaviour = behaviourFor(mode)
        // Clears cached highlights/annotations and re-queries the providers for the newly
        // selected mode, mirroring the native sample switching ScanFragment instances.
        view?.reset()
    }

    private fun behaviourFor(mode: ArMode): ArScanBehaviour = when (mode) {
        ArMode.Highlights -> ArHighlightsBehaviour()
        ArMode.Annotations -> ArAnnotationsBehaviour()
        ArMode.Popovers -> ArPopoversBehaviour(resetView = { view?.reset() })
        ArMode.StatusIcons -> ArStatusIconsBehaviour()
    }

    fun onResume() { view?.start() }
    fun onPause() { view?.pause() }

    fun dispose() {
        view?.stop()
        view = null
        observerScope.cancel()
    }

    // --- BarcodeArHighlightProvider / BarcodeArAnnotationProvider ---

    override fun highlightForBarcode(barcode: Barcode, callback: (BarcodeArHighlight?) -> Unit) {
        behaviour.highlightForBarcode(barcode, callback)
    }

    override fun annotationForBarcode(barcode: Barcode, callback: (BarcodeArAnnotation?) -> Unit) {
        behaviour.annotationForBarcode(barcode, callback)
    }

    // --- BarcodeArViewUiListener ---

    override fun onHighlightForBarcodeTapped(barcodeAr: BarcodeAr, barcode: Barcode, highlight: BarcodeArHighlight) {
        behaviour.onHighlightForBarcodeTapped(barcodeAr, barcode, highlight)
    }
}
