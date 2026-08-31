/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.barcode.data.SymbologyDescription
import com.kmp.datacapture.barcode.find.BarcodeFind
import com.kmp.datacapture.barcode.find.BarcodeFindItem
import com.kmp.datacapture.barcode.find.BarcodeFindItemContent
import com.kmp.datacapture.barcode.find.BarcodeFindItemSearchOptions
import com.kmp.datacapture.barcode.find.BarcodeFindListener
import com.kmp.datacapture.barcode.find.BarcodeFindSettings
import com.kmp.datacapture.barcode.find.BarcodeFindViewSettings
import com.kmp.datacapture.barcode.spark.SparkScan
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model. Owns every Scandit SDK *mode* the app needs
// (SparkScan for the Search phase, BarcodeFind for the Find phase), exposes
// the [state] StateFlow each platform observes, and accepts platform intents
// through [onEvent].
//
// The platform-specific *views* (SparkScanView, BarcodeFindView) are NOT
// owned here: each view's constructor takes a platform Context/host, so the
// host builds it and registers it back via [registerSparkScanView] /
// [registerBarcodeFindView] (mirrors the KMP DebugApp pattern).
//
// Flow (mirrors the native SearchAndFindSample):
//   Home -> Search (SparkScan): every scanned barcode is appended to
//           [scannedBarcodes] -> tap FIND -> Find (BarcodeFind) seeded with
//           those barcodes -> finish -> back to Search (items kept).
class SearchAndFindScreenModel : SparkScanListener, BarcodeFindListener {

    private val _state = MutableStateFlow<SearchAndFindUiState>(SearchAndFindUiState.Home)
    val state: StateFlow<SearchAndFindUiState> = _state.asStateFlow()

    // One-shot events (navigation / errors) the host consumes once.
    private val _events = MutableSharedFlow<SearchAndFindEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SearchAndFindEvent> = _events.asSharedFlow()

    // The barcodes captured during the Search phase. Backs both the Search
    // list UI and the Find item list.
    private val _scannedBarcodes = mutableListOf<SearchScannedBarcode>()
    val scannedBarcodes: List<SearchScannedBarcode> get() = _scannedBarcodes.toList()

    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Swift-friendly subscription on top of the StateFlow (Android collects
    // [state] directly via Compose's `collectAsState`).
    fun observeState(onEach: (SearchAndFindUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    // --- Search phase (SparkScan) ---------------------------------------

    val sparkScan: SparkScan =
        SparkScan(buildSparkScanSettings()).also { it.addListener(this) }

    // View settings are built here (Kotlin) and read by each platform host, so
    // Swift never has to reach the Kotlin companion factory (which it can't).
    val sparkScanViewSettings: SparkScanViewSettings =
        SparkScanViewSettings.sparkScanViewSettings()

    val barcodeFindViewSettings: BarcodeFindViewSettings =
        BarcodeFindViewSettings.barcodeFindViewSettings()

    private var sparkScanViewRef: SparkScanViewHandle? = null

    // --- Find phase (BarcodeFind) ---------------------------------------
    // Built lazily on startFind() so the item list reflects everything that
    // was scanned in the Search phase.

    private var barcodeFind: BarcodeFind? = null
    private var barcodeFindViewRef: BarcodeFindViewHandle? = null

    // SparkScanListener -------------------------------------------------

    override fun onBarcodeScanned(
        sparkScan: SparkScan,
        session: SparkScanSession,
        frameData: FrameData,
    ) {
        val barcode = session.newlyRecognizedBarcode
        val data = barcode?.data
        if (barcode != null && !data.isNullOrEmpty()) {
            val symbologyName = SymbologyDescription.forSymbology(barcode.symbology).readableName
            val image = cropBarcode(barcode, frameData)
            onBarcodeScanned(SearchScannedBarcode(data, symbologyName, listOfNotNull(image)))
        }
    }

    override fun onSessionUpdated(
        sparkScan: SparkScan,
        session: SparkScanSession,
        frameData: FrameData,
    ) {
        // no-op
    }

    // Appends a freshly scanned barcode and refreshes the Search list.
    fun onBarcodeScanned(barcode: SearchScannedBarcode) {
        _scannedBarcodes.add(barcode)
        if (_state.value is SearchAndFindUiState.Search) {
            _state.value = SearchAndFindUiState.Search(scannedBarcodes)
        }
    }

    // Empties the scanned list and resets the Search list.
    fun clearList() {
        _scannedBarcodes.clear()
        if (_state.value is SearchAndFindUiState.Search) {
            _state.value = SearchAndFindUiState.Search(emptyList())
        }
    }

    // BarcodeFindListener -----------------------------------------------

    override fun onSearchPaused(foundItems: Set<BarcodeFindItem>) {
        // no-op
    }

    override fun onSearchStarted() {
        // no-op
    }

    override fun onSearchStopped(foundItems: Set<BarcodeFindItem>) {
        // no-op: the finish button / back press drives the return to Search via
        // FinishFind; stopSearching() during teardown also fires this.
    }

    // Builds the BarcodeFind mode from the scanned barcodes and moves to the
    // Find phase. The platform host then constructs a BarcodeFindView for
    // [barcodeFind] and registers it via [registerBarcodeFindView].
    fun startFind() {
        // Dedupe by barcode data: the same code scanned twice is one search item.
        val items = _scannedBarcodes
            .distinctBy { it.data }
            .map { scanned ->
                BarcodeFindItem(
                    BarcodeFindItemSearchOptions(scanned.data),
                    BarcodeFindItemContent(null, null, null),
                )
            }.toSet()

        val find = BarcodeFind.forContext(dataCaptureContext, buildBarcodeFindSettings()).also { mode ->
            mode.addListener(this)
            mode.setItemList(items)
        }
        barcodeFind = find
        _state.value = SearchAndFindUiState.Find
        _events.tryEmit(SearchAndFindEvent.NavigateToFind)
    }

    fun barcodeFindOrNull(): BarcodeFind? = barcodeFind

    // BarcodeFind finished: tear the mode down and return to a fresh, empty
    // Search (no result screen; leaving Find clears the scanned list).
    fun finishFind() {
        disposeFind()
        _scannedBarcodes.clear()
        _state.value = SearchAndFindUiState.Search(emptyList())
    }

    // Intents ------------------------------------------------------------

    fun onEvent(event: SearchAndFindEvent) {
        when (event) {
            SearchAndFindEvent.NavigateToSearch -> {
                _scannedBarcodes.clear()
                _state.value = SearchAndFindUiState.Search(emptyList())
            }
            SearchAndFindEvent.NavigateToFind -> startFind()
            SearchAndFindEvent.FinishFind -> finishFind()
            SearchAndFindEvent.NavigateBack -> {
                // Leaving Search always discards the scanned list.
                _scannedBarcodes.clear()
                _state.value = SearchAndFindUiState.Home
            }
            is SearchAndFindEvent.ShowError -> _events.tryEmit(event)
        }
    }

    // View registration (platform-side construction) --------------------

    fun registerSparkScanView(handle: SparkScanViewHandle) {
        sparkScanViewRef = handle
    }

    fun unregisterSparkScanView() {
        sparkScanViewRef = null
    }

    fun registerBarcodeFindView(handle: BarcodeFindViewHandle) {
        barcodeFindViewRef = handle
    }

    // Lifecycle ----------------------------------------------------------

    fun onSearchResumed() {
        sparkScanViewRef?.onResume()
    }

    fun onSearchPaused() {
        sparkScanViewRef?.onPause()
    }

    fun onFindResumed() {
        barcodeFindViewRef?.onResume()
        barcodeFindViewRef?.startSearching()
    }

    fun onFindPaused() {
        barcodeFindViewRef?.onPause()
    }

    fun disposeFind() {
        // Remove the listener first so the stopSearching() teardown below
        // doesn't re-enter onSearchStopped while we're tearing the mode down.
        barcodeFind?.removeListener(this)
        barcodeFindViewRef?.stopSearching()
        barcodeFind?.stop()
        barcodeFind?.let { dataCaptureContext.removeMode(it) }
        barcodeFind = null
        barcodeFindViewRef = null
    }

    fun dispose() {
        disposeFind()
        sparkScan.removeListener(this)
        sparkScanViewRef = null
        observerScope.cancel()
    }

    private fun buildSparkScanSettings(): SparkScanSettings =
        SparkScanSettings.sparkScanSettings().also { settings ->
            settings.enableSymbologies(SEARCH_SYMBOLOGIES)
            // Match the native sample: CODE39 barcodes here are 7–20 symbols long.
            settings.getSymbologySettings(Symbology.CODE39).activeSymbolCounts = CODE39_SYMBOL_COUNTS
        }

    private fun buildBarcodeFindSettings(): BarcodeFindSettings =
        BarcodeFindSettings.barcodeFindSettings().also { settings ->
            settings.enableSymbologies(SEARCH_SYMBOLOGIES)
            settings.getSymbologySettings(Symbology.CODE39).activeSymbolCounts = CODE39_SYMBOL_COUNTS
        }

    companion object {
        private val SEARCH_SYMBOLOGIES = setOf(
            Symbology.EAN8,
            Symbology.EAN13_UPCA,
            Symbology.UPCE,
            Symbology.CODE128,
            Symbology.CODE39,
            Symbology.DATA_MATRIX,
        )

        private val CODE39_SYMBOL_COUNTS: Set<Short> = (7..20).map { it.toShort() }.toSet()

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}

// Platform-agnostic handles the host registers so the screen model can drive
// view lifecycle without depending on a platform view type. The Android host
// wraps its SparkScanView / BarcodeFindView in these.
interface SparkScanViewHandle {
    fun onResume()
    fun onPause()
}

interface BarcodeFindViewHandle {
    fun onResume()
    fun onPause()
    fun startSearching()
    fun stopSearching()
}
