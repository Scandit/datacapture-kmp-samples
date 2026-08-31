/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.barcode.pick.BarcodePick
import com.kmp.datacapture.barcode.pick.BarcodePickActionCallback
import com.kmp.datacapture.barcode.pick.BarcodePickActionListener
import com.kmp.datacapture.barcode.pick.BarcodePickAsyncMapperProductProvider
import com.kmp.datacapture.barcode.pick.BarcodePickAsyncMapperProductProviderCallback
import com.kmp.datacapture.barcode.pick.BarcodePickListener
import com.kmp.datacapture.barcode.pick.BarcodePickProductProviderCallback
import com.kmp.datacapture.barcode.pick.BarcodePickScanningListener
import com.kmp.datacapture.barcode.pick.BarcodePickScanningSession
import com.kmp.datacapture.barcode.pick.BarcodePickSession
import com.kmp.datacapture.barcode.pick.BarcodePickSettings
import com.kmp.datacapture.barcode.pick.BarcodePickView
import com.kmp.datacapture.barcode.pick.BarcodePickViewSettings
import com.kmp.datacapture.barcode.pick.BarcodePickViewUiListener
import com.kmp.datacapture.core.capture.DataCaptureContext
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sample-wide MVI screen model. Owns the BarcodePick mode for the whole
// sample, exposes the [state] StateFlow each platform observes, and accepts
// platform intents through [onEvent].
//
// The platform-specific BarcodePickView is NOT owned here: its constructor
// takes a platform Context/host, so the host builds it and registers it back
// via [registerBarcodePickView] (mirrors the KMP DebugApp pattern and the
// sibling SearchAndFindSample's SparkScanViewHandle).
//
// Flow (mirrors the native RestockingSample):
//   Pick (BarcodePick): scan and pick items off the pick list -> tap the
//   view's built-in finish button -> Result: a static Picklist + Inventory
//   breakdown of what was picked/scanned.
class RestockingScreenModel :
    BarcodePickActionListener,
    BarcodePickListener,
    BarcodePickScanningListener,
    BarcodePickViewUiListener {

    private val _state = MutableStateFlow<RestockingUiState>(RestockingUiState.Pick)
    val state: StateFlow<RestockingUiState> = _state.asStateFlow()

    private val productManager = ProductManager()

    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Swift-friendly subscription on top of the StateFlow (Android collects
    // [state] directly via Compose's `collectAsState`).
    fun observeState(onEach: (RestockingUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    // The BarcodePick mode is rebuilt on "Finish pickup" (see [onEvent]) to
    // start a fresh picking session, mirroring the native sample which creates
    // a new PickFragment/BarcodePick each time. "Continue scanning" reuses the
    // same instance so the in-progress session is preserved.
    var barcodePick: BarcodePick = buildBarcodePick()
        private set

    private fun buildProductProvider(): BarcodePickAsyncMapperProductProvider =
        BarcodePickAsyncMapperProductProvider(
            productManager.getBarcodePickProducts(),
            object : BarcodePickAsyncMapperProductProviderCallback {
                override fun mapItems(itemsData: Set<String>, callback: BarcodePickProductProviderCallback) {
                    productManager.mapItems(itemsData, callback)
                }
            },
        )

    private fun buildBarcodePick(): BarcodePick =
        BarcodePick.forContext(dataCaptureContext, buildBarcodePickSettings(), buildProductProvider()).also {
            it.addListener(this)
            it.addScanningListener(this)
        }

    // View settings are built here (Kotlin) and read by each platform host, so
    // Swift never has to reach the Kotlin companion factory (which it can't).
    val barcodePickViewSettings: BarcodePickViewSettings =
        BarcodePickViewSettings.barcodePickViewSettings()

    private var barcodePickViewRef: BarcodePickViewHandle? = null

    // BarcodePickActionListener ------------------------------------------

    // Pick/unpick complete asynchronously with a short randomized delay, so the
    // BarcodePick UI shows its in-progress spinner — mirrors the native
    // sample's simulated database/network round-trip (ProductManager
    // pickItemAsync/unpickItemAsync).
    override fun onPick(itemData: String, callback: BarcodePickActionCallback) {
        observerScope.launch {
            delay(PICK_ACTION_DELAY_MS_MIN + Random.nextLong(PICK_ACTION_DELAY_MS_JITTER))
            callback.onFinish(productManager.verifyProductInDatabase(itemData))
        }
    }

    override fun onUnpick(itemData: String, callback: BarcodePickActionCallback) {
        observerScope.launch {
            delay(PICK_ACTION_DELAY_MS_MIN + Random.nextLong(PICK_ACTION_DELAY_MS_JITTER))
            callback.onFinish(productManager.verifyProductInDatabase(itemData))
        }
    }

    // BarcodePickListener --------------------------------------------------

    override fun onSessionUpdated(barcodePick: BarcodePick, session: BarcodePickSession) {
        // no-op: tracked/added items aren't surfaced in this sample's UI.
    }

    // BarcodePickScanningListener -------------------------------------------

    override fun onScanningSessionUpdated(barcodePick: BarcodePick, session: BarcodePickScanningSession) {
        productManager.setAllScannedCodes(session.scannedItems)
        productManager.setAllPickedCodes(session.pickedItems)
    }

    override fun onScanningSessionCompleted(barcodePick: BarcodePick, session: BarcodePickScanningSession) {
        productManager.setAllScannedCodes(session.scannedItems)
        productManager.setAllPickedCodes(session.pickedItems)
    }

    // BarcodePickViewUiListener ---------------------------------------------

    // The built-in finish button was tapped: leave Pick and show the result
    // breakdown built from everything scanned/picked so far.
    override fun onFinishButtonTapped(view: BarcodePickView) {
        _state.value = RestockingUiState.Result(productManager.getPickResultContent())
    }

    // Intents ----------------------------------------------------------

    fun onEvent(event: RestockingEvent) {
        when (event) {
            // Return to the same picking session (state preserved).
            RestockingEvent.ContinueScanning -> {
                _state.value = RestockingUiState.Pick
            }
            // Clear everything picked/scanned and start a fresh picking
            // session, mirroring the native sample's "Finish pickup".
            RestockingEvent.FinishPickup -> {
                productManager.clearPickedAndScanned()
                barcodePick.removeListener(this)
                barcodePick.removeScanningListener(this)
                barcodePick = buildBarcodePick()
                _state.value = RestockingUiState.Pick
            }
        }
    }

    // View registration (platform-side construction) --------------------

    fun registerBarcodePickView(handle: BarcodePickViewHandle) {
        barcodePickViewRef = handle
    }

    fun unregisterBarcodePickView() {
        barcodePickViewRef = null
    }

    // Lifecycle ----------------------------------------------------------

    fun onPickResumed() {
        barcodePickViewRef?.onResume()
    }

    fun onPickPaused() {
        barcodePickViewRef?.onPause()
    }

    fun dispose() {
        barcodePick.removeListener(this)
        barcodePick.removeScanningListener(this)
        barcodePickViewRef?.stop()
        barcodePickViewRef = null
        observerScope.cancel()
    }

    private fun buildBarcodePickSettings(): BarcodePickSettings =
        BarcodePickSettings.barcodePickSettings().also { settings ->
            PICK_SYMBOLOGIES.forEach { symbology -> settings.enableSymbology(symbology, true) }
        }

    companion object {
        private val PICK_SYMBOLOGIES = setOf(
            Symbology.EAN13_UPCA,
            Symbology.EAN8,
            Symbology.CODE39,
            Symbology.CODE128,
            Symbology.UPCE,
        )

        // Simulated database round-trip for pick/unpick: 250-750ms, matching
        // the native sample's ProductManager (250 + Random(500)).
        private const val PICK_ACTION_DELAY_MS_MIN = 250L
        private const val PICK_ACTION_DELAY_MS_JITTER = 500L

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}

// One-shot / simple intents the host sends back to the screen model. State
// that the screen *renders* lives in [RestockingUiState]; this is for the
// single "go back and keep picking" action off the Result screen.
sealed interface RestockingEvent {
    data object ContinueScanning : RestockingEvent
    data object FinishPickup : RestockingEvent
}

// Platform-agnostic handle the host registers so the screen model can drive
// view lifecycle without depending on a platform view type. The Android host
// wraps its BarcodePickView in one of these. [start]/[stop] are the scan axis;
// [onResume]/[onPause] are the separate Android view lifecycle axis, kept
// independent so backgrounding/foregrounding the app does not itself unfreeze
// or otherwise change scanning state.
interface BarcodePickViewHandle {
    fun start()
    fun onResume()
    fun onPause()
    fun stop()
}
