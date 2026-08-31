/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscanbubblessample

import com.kmp.datacapture.barcode.batch.BarcodeBatch
import com.kmp.datacapture.barcode.batch.BarcodeBatchAdvancedOverlay
import com.kmp.datacapture.barcode.batch.BarcodeBatchAdvancedOverlayListener
import com.kmp.datacapture.barcode.batch.BarcodeBatchBasicOverlay
import com.kmp.datacapture.barcode.batch.BarcodeBatchListener
import com.kmp.datacapture.barcode.batch.BarcodeBatchSession
import com.kmp.datacapture.barcode.batch.BarcodeBatchSettings
import com.kmp.datacapture.barcode.batch.TrackedBarcode
import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.common.geometry.Anchor
import com.kmp.datacapture.core.common.geometry.FloatWithUnit
import com.kmp.datacapture.core.common.geometry.MeasureUnit
import com.kmp.datacapture.core.common.geometry.PointWithUnit
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.CameraSettings
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.source.VideoResolution
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.LogoStyle
import com.kmp.datacapture.core.ui.NativeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model for MatrixScan Bubbles. Owns every Scandit SDK
// object the AR overlay needs, exposes the [frozen] StateFlow each platform
// observes for the freeze button, and accepts platform intents through
// [onEvent].
//
// The shared layer owns *all* SDK wiring and the freeze state machine; the
// platform hosts only (a) embed the DataCaptureView and (b) build the per-
// barcode bubble view, since that view is a native widget (`android.view.View`
// / `UIView`) that can't be constructed in commonMain. The platform passes a
// [bubbleViewFactory] into [setupDataCaptureView]; the model wires it into the
// BarcodeBatchAdvancedOverlay.
class MatrixScanBubblesScreenModel : BarcodeBatchListener, BarcodeBatchAdvancedOverlayListener {

    // Freeze state: true once the user taps the freeze button. While frozen the
    // camera + mode are paused but the bubbles stay on screen, mirroring the
    // native MatrixScanBubblesSample.
    private val _frozen = MutableStateFlow(false)
    val frozen: StateFlow<Boolean> = _frozen.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [frozen] directly via Compose's `collectAsState`; iOS lacks a
    // built-in StateFlow → SwiftUI bridge, so the host calls [observeFrozen]
    // and stores the returned [FlowCancellable] for the view's lifetime.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeFrozen(onEach: (Boolean) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            frozen.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    // Match the native sample: run the camera at UHD4K so small/distant barcodes
    // track well enough to anchor a bubble. KMP's BarcodeBatch has no
    // createRecommendedCameraSettings(), so build the settings directly.
    private val cameraSettings: CameraSettings =
        CameraSettings().apply { preferredResolution = VideoResolution.UHD4K }

    private val camera: Camera? =
        Camera.getDefaultCamera(cameraSettings)?.also {
            dataCaptureContext.setFrameSource(it)
        }

    // The barcode batch process is configured through settings which are then
    // applied to the mode instance. For the purpose of this sample we enable a
    // generous set of symbologies; in a real app enable only what you need, as
    // every extra symbology costs processing time.
    private val settings: BarcodeBatchSettings =
        BarcodeBatchSettings.barcodeBatchSettings().also {
            it.enableSymbology(Symbology.EAN13_UPCA, true)
            it.enableSymbology(Symbology.EAN8, true)
            it.enableSymbology(Symbology.UPCE, true)
            it.enableSymbology(Symbology.CODE39, true)
            it.enableSymbology(Symbology.CODE128, true)
        }

    // Public so the iOS host can wire the SDK's Swift bridge to it once.
    val barcodeBatch: BarcodeBatch =
        BarcodeBatch.forContext(dataCaptureContext, settings).also {
            it.addListener(this)
        }

    // The view the size check maps barcode coordinates against. Set in
    // [setupDataCaptureView]; needed by [shouldShowBubble].
    private var dataCaptureView: DataCaptureView? = null

    // The advanced overlay producing the bubbles. Kept so dispose() can detach
    // its listener.
    private var advancedOverlay: BarcodeBatchAdvancedOverlay? = null

    // Platform-supplied factory that builds (or recycles) the native bubble
    // view for a tracked barcode.
    private var bubbleViewFactory: ((TrackedBarcode, BubbleData) -> NativeView?)? = null

    // Set by the host so it can drop its cached bubble views when barcodes leave
    // tracking — the SDK detaches the view itself, but the host must release its
    // own reference or the cache grows unbounded.
    var onTrackedBarcodesRemoved: ((List<Int>) -> Unit)? = null

    // The view width (px) used by the size threshold. Set by the host once the
    // view has been laid out.
    private var viewWidth: Float = 0f

    fun setViewWidth(width: Float) {
        viewWidth = width
    }

    fun setupDataCaptureView(
        view: DataCaptureView,
        bubbleViewFactory: (TrackedBarcode, BubbleData) -> NativeView?,
    ): DataCaptureView {
        view.logoStyle = LogoStyle.MINIMAL
        dataCaptureView = view
        this.bubbleViewFactory = bubbleViewFactory

        // A basic overlay highlights every tracked barcode with the default dot.
        val basicOverlay = BarcodeBatchBasicOverlay.withBarcodeBatchForView(barcodeBatch, view)
        view.addOverlay(basicOverlay)

        // An advanced overlay draws the AR bubbles on top of the barcodes.
        val overlay = BarcodeBatchAdvancedOverlay.withBarcodeBatch(barcodeBatch).also {
            it.listener = this
        }
        advancedOverlay = overlay
        view.addOverlay(overlay)
        return view
    }

    fun onEvent(event: MatrixScanBubblesEvent) {
        when (event) {
            MatrixScanBubblesEvent.STARTED -> {
                if (_frozen.value) return
                camera?.switchToDesiredState(FrameSourceState.ON)
                barcodeBatch.isEnabled = true
            }
            MatrixScanBubblesEvent.STOPPED -> {
                if (_frozen.value) return
                barcodeBatch.isEnabled = false
                camera?.switchToDesiredState(FrameSourceState.OFF)
            }
            MatrixScanBubblesEvent.TOGGLE_FREEZE -> {
                if (_frozen.value) {
                    // Unfreeze: resume the camera + mode. New frames replace the
                    // frozen bubbles.
                    _frozen.value = false
                    camera?.switchToDesiredState(FrameSourceState.ON)
                    barcodeBatch.isEnabled = true
                } else {
                    // Freeze: pause the mode + stop the camera but keep the
                    // bubbles currently drawn by the advanced overlay.
                    _frozen.value = true
                    barcodeBatch.isEnabled = false
                    camera?.switchToDesiredState(FrameSourceState.OFF)
                }
            }
        }
    }

    fun isFrozen(): Boolean = _frozen.value

    // --- BarcodeBatchListener ---

    override fun onSessionUpdated(
        barcodeBatch: BarcodeBatch,
        session: BarcodeBatchSession,
        frameData: FrameData,
    ) {
        // The advanced overlay drives bubble creation/positioning through the
        // listener callbacks below. The only per-frame work is releasing host
        // bubble caches for barcodes that left tracking.
        val removed = session.removedTrackedBarcodes
        if (removed.isNotEmpty()) {
            // onSessionUpdated runs on an SDK worker thread; the host's bubble
            // cache is touched by the (main-thread) view factory, so hop to main.
            observerScope.launch { onTrackedBarcodesRemoved?.invoke(removed) }
        }
    }

    // --- BarcodeBatchAdvancedOverlayListener ---

    override fun viewForTrackedBarcode(
        overlay: BarcodeBatchAdvancedOverlay,
        trackedBarcode: TrackedBarcode,
    ): NativeView? {
        if (!shouldShowBubble(trackedBarcode)) return null
        val code = trackedBarcode.barcode.data ?: ""
        return bubbleViewFactory?.invoke(trackedBarcode, BubbleData.forBarcode(code))
    }

    override fun anchorForTrackedBarcode(
        overlay: BarcodeBatchAdvancedOverlay,
        trackedBarcode: TrackedBarcode,
    ): Anchor = Anchor.TOP_CENTER

    override fun offsetForTrackedBarcode(
        overlay: BarcodeBatchAdvancedOverlay,
        trackedBarcode: TrackedBarcode,
    ): PointWithUnit = PointWithUnit(
        // Centre the view horizontally over the barcode and lift it one
        // bubble-height above — matches the native sample.
        FloatWithUnit(0f, MeasureUnit.FRACTION),
        FloatWithUnit(-1f, MeasureUnit.FRACTION),
    )

    // Show the bubble only if the barcode occupies >= 10% of the view width,
    // mirroring the native BubbleSizeManager. Coordinates are mapped from
    // frame space to view space first.
    private fun shouldShowBubble(trackedBarcode: TrackedBarcode): Boolean {
        val view = dataCaptureView ?: return false
        if (viewWidth <= 0f) return true
        val loc = view.mapFrameQuadrilateralToView(trackedBarcode.location)
        val avgWidth =
            ((loc.topRight.x - loc.topLeft.x) + (loc.bottomRight.x - loc.bottomLeft.x)) / 2f
        return (avgWidth / viewWidth) >= SCREEN_PERCENTAGE_WIDTH_REQUIRED
    }

    fun dispose() {
        barcodeBatch.isEnabled = false
        barcodeBatch.removeListener(this)
        advancedOverlay?.listener = null
        dataCaptureContext.removeMode(barcodeBatch)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    companion object {
        private const val SCREEN_PERCENTAGE_WIDTH_REQUIRED = 0.1f

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
