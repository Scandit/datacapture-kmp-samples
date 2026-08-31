/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.barcode.spark.SparkScan
import com.kmp.datacapture.barcode.spark.SparkScanBarcodeErrorFeedback
import com.kmp.datacapture.barcode.spark.SparkScanBarcodeFeedback
import com.kmp.datacapture.barcode.spark.SparkScanBarcodeSuccessFeedback
import com.kmp.datacapture.barcode.spark.SparkScanFeedbackDelegate
import com.kmp.datacapture.barcode.spark.SparkScanListener
import com.kmp.datacapture.barcode.spark.SparkScanSession
import com.kmp.datacapture.barcode.spark.SparkScanSettings
import com.kmp.datacapture.barcode.spark.SparkScanViewSettings
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.data.FrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sample-wide MVI screen model. Owns every Scandit SDK object the
// ListBuilding screen needs (DataCaptureContext, SparkScan + its settings,
// the SparkScanViewSettings the platform view factory consumes), implements
// the SparkScanListener + SparkScanFeedbackDelegate, and exposes the
// scanned-item list as a StateFlow each platform observes.
class ListBuildingScreenModel : SparkScanListener, SparkScanFeedbackDelegate {

    private val _items = MutableStateFlow<List<ListBuildingEntry>>(emptyList())

    /** Newest-first list of scanned barcodes. */
    val items: StateFlow<List<ListBuildingEntry>> = _items.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)

    /** Total number of scans (sum of all item quantities). */
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    // Pure, SDK-free list model holding the dedup/quantity/ordering rules.
    private val scannedItems = ScannedItemList()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [items] / [scannedCount] directly via Compose's collectAsState;
    // iOS lacks a built-in StateFlow → SwiftUI bridge, so the host calls
    // [observeItems] and stores the returned [FlowCancellable].
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Off-main scope for the per-scan thumbnail crop (a full-frame decode);
    // keeps the SDK scan-callback thread free, like the native sample's
    // background executor.
    private val workScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun observeItems(onEach: (List<ListBuildingEntry>) -> Unit): FlowCancellable {
        val job = observerScope.launch { items.collect { onEach(it) } }
        return FlowCancellable(job)
    }

    fun observeScannedCount(onEach: (Int) -> Unit): FlowCancellable {
        val job = observerScope.launch { scannedCount.collect { onEach(it) } }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    // For the purpose of this sample we enable a generous set of symbologies.
    // In your own app only enable the symbologies you require — every extra
    // symbology has an impact on processing times.
    private val settings: SparkScanSettings =
        SparkScanSettings.sparkScanSettings().also {
            it.enableSymbologies(
                setOf(
                    Symbology.EAN13_UPCA,
                    Symbology.EAN8,
                    Symbology.UPCE,
                    Symbology.CODE39,
                    Symbology.CODE128,
                    Symbology.INTERLEAVED_TWO_OF_FIVE,
                ),
            )
            // CODE39 can encode variable-length data; widen its active symbol
            // counts so longer codes outside the default range still scan.
            it.getSymbologySettings(Symbology.CODE39).activeSymbolCounts =
                (7..20).map { count -> count.toShort() }.toSet()
        }

    val sparkScan: SparkScan = SparkScan(settings).also {
        it.addListener(this)
    }

    /** The view settings the platform `rememberSparkScanView` factory consumes. */
    val sparkScanViewSettings: SparkScanViewSettings =
        SparkScanViewSettings.sparkScanViewSettings()

    private fun isValidBarcode(barcode: Barcode): Boolean =
        barcode.data != null && barcode.data != "123456789"

    override fun onBarcodeScanned(
        sparkScan: SparkScan,
        session: SparkScanSession,
        frameData: FrameData,
    ) {
        val barcode = session.newlyRecognizedBarcode ?: return
        if (!isValidBarcode(barcode)) return

        val data = barcode.data ?: return
        val symbology = barcode.symbology.name

        // Crop off the scan-callback thread, then mutate the list on Main (the
        // native sample does the same: background crop → runOnUiThread add).
        // Retain the frame so its native buffer outlives the callback.
        frameData.retain()
        workScope.launch {
            val image = cropBarcode(barcode, frameData)
            frameData.release()
            withContext(Dispatchers.Main) {
                scannedItems.record(data, symbology, image)
                publish()
            }
        }
    }

    override fun onSessionUpdated(
        sparkScan: SparkScan,
        session: SparkScanSession,
        frameData: FrameData,
    ) = Unit

    override fun getFeedbackForBarcode(barcode: Barcode): SparkScanBarcodeFeedback? =
        if (isValidBarcode(barcode)) {
            SparkScanBarcodeSuccessFeedback()
        } else {
            SparkScanBarcodeErrorFeedback(
                errorMessage = "Wrong barcode!",
                resumeCapturingDelay = WRONG_BARCODE_RESUME_DELAY_MS,
            )
        }

    fun clearList() {
        scannedItems.clear()
        publish()
    }

    private fun publish() {
        _items.value = scannedItems.items()
        _scannedCount.value = scannedItems.scannedCount()
    }

    fun dispose() {
        // SparkScan is not a context mode -- it is bound through SparkScanView, so there is
        // nothing to remove from the DataCaptureContext here.
        sparkScan.removeListener(this)
        observerScope.cancel()
        workScope.cancel()
    }

    companion object {
        // Matches the native sample: a rejected barcode keeps the error feedback
        // visible and pauses capture for 60 seconds.
        private const val WRONG_BARCODE_RESUME_DELAY_MS = 60_000L

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
