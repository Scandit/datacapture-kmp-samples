/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.matrixscancountsimplesample

import com.kmp.datacapture.barcode.count.BarcodeCount
import com.kmp.datacapture.barcode.count.BarcodeCountListener
import com.kmp.datacapture.barcode.count.BarcodeCountSession
import com.kmp.datacapture.barcode.count.BarcodeCountSettings
import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sample-wide MVI screen model. Owns the [DataCaptureContext] and [BarcodeCount]
 * mode for the whole sample lifetime (constructed once, held by the app-level
 * host on both platforms), so navigating between the Scanner and Results
 * screens never tears down or recreates the scanning session — unlike the
 * native Android sample, which persists the session across Activities via a
 * static `BarcodeManager` + `additionalBarcodes` reload dance. Keeping one
 * long-lived model sidesteps that workaround entirely (a deliberate,
 * intentional UX-preserving simplification for the KMP port).
 *
 * Registers a [BarcodeCountListener] whose `onScan` aggregates every
 * recognized barcode (by decoded data) into [CountedBarcode] rows, exposed as
 * [items] / [totalCount].
 */
class MatrixScanCountScreenModel : BarcodeCountListener {

    val dataCaptureContext: DataCaptureContext = DataCaptureContext.forLicenseKey(LICENSE_KEY)

    // For the purpose of this sample we enable a generous set of symbologies.
    // In your own app only enable the symbologies you require — every extra
    // symbology has an impact on processing times.
    val settings: BarcodeCountSettings = BarcodeCountSettings.barcodeCountSettings().also {
        it.enableSymbologies(
            setOf(
                Symbology.EAN13_UPCA,
                Symbology.EAN8,
                Symbology.UPCE,
                Symbology.CODE39,
                Symbology.CODE128,
            ),
        )
    }

    val barcodeCount: BarcodeCount = BarcodeCount.forContext(dataCaptureContext, settings).also {
        it.addListener(this)
    }

    // Use the recommended camera settings for the BarcodeCount mode and set the
    // default camera as the context's frame source. The camera is off by
    // default and must be switched on to start streaming frames for
    // recognition — see [resumeFrameSource] / [pauseFrameSource].
    private val camera: Camera? =
        Camera.getDefaultCamera(BarcodeCount.createRecommendedCameraSettings())?.also {
            dataCaptureContext.setFrameSource(it)
        }

    /**
     * Enables the mode and switches the camera on to start scanning. Called by
     * each platform host when the Scanner screen appears (mirrors the native
     * sample's `onResume`).
     */
    fun resumeFrameSource() {
        barcodeCount.isEnabled = true
        camera?.switchToDesiredState(FrameSourceState.ON)
    }

    /**
     * Switches the camera off to stop streaming frames. Called by each platform
     * host when the Scanner screen leaves the foreground (mirrors the native
     * sample's `onPause`).
     */
    fun pauseFrameSource() {
        camera?.switchToDesiredState(FrameSourceState.OFF)
    }

    private val _items = MutableStateFlow<List<CountedBarcode>>(emptyList())

    /** Aggregated scan results, one row per distinct barcode data. */
    val items: StateFlow<List<CountedBarcode>> = _items.asStateFlow()

    private val _totalCount = MutableStateFlow(0)

    /** Sum of every row's quantity — the header count shown on the Results screen. */
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    // Swift-friendly subscription on top of the StateFlows. Android consumers
    // collect [items] / [totalCount] directly via Compose's collectAsState;
    // iOS lacks a built-in StateFlow -> SwiftUI bridge, so the host calls
    // [observeItems] / [observeTotalCount] and stores the returned
    // [FlowCancellable].
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeItems(onEach: (List<CountedBarcode>) -> Unit): FlowCancellable {
        val job = observerScope.launch { items.collect { onEach(it) } }
        return FlowCancellable(job)
    }

    fun observeTotalCount(onEach: (Int) -> Unit): FlowCancellable {
        val job = observerScope.launch { totalCount.collect { onEach(it) } }
        return FlowCancellable(job)
    }

    // The camera and mode keep running while the Results screen is shown, so
    // gate list updates: freeze the published list while Results is visible so
    // it stays a stable snapshot (and a Clear List isn't immediately refilled
    // by the still-running scanner). Re-enabled on return to the Scanner.
    private var updatesEnabled = true

    fun setUpdatesEnabled(enabled: Boolean) {
        updatesEnabled = enabled
    }

    override fun onScan(barcodeCount: BarcodeCount, session: BarcodeCountSession, frameData: FrameData) {
        if (!updatesEnabled) return
        val scans = (session.recognizedBarcodes + session.additionalBarcodes)
            .mapNotNull { barcode -> barcode.data?.let { it to barcode.symbology.name } }
        publish(CountedBarcodeAggregator.aggregate(scans))
    }

    /** Clears the aggregated results and resets the underlying scanning session. */
    fun resetSession() {
        barcodeCount.clearAdditionalBarcodes()
        barcodeCount.reset()
        publish(emptyList())
    }

    private fun publish(rows: List<CountedBarcode>) {
        _items.value = rows
        _totalCount.value = rows.sumOf { it.quantity }
    }

    fun dispose() {
        // The camera streams independently of the mode; switch it off here or
        // it stays held open after the model is released (e.g. across an
        // Activity config-change), risking "camera already in use" / drain.
        camera?.switchToDesiredState(FrameSourceState.OFF)
        barcodeCount.removeListener(this)
        dataCaptureContext.removeMode(barcodeCount)
        observerScope.cancel()
    }
}
