/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.barcodeselectionsimplesample

import com.kmp.datacapture.barcode.data.Symbology
import com.kmp.datacapture.barcode.selection.BarcodeSelection
import com.kmp.datacapture.barcode.selection.BarcodeSelectionAimerSelection
import com.kmp.datacapture.barcode.selection.BarcodeSelectionBasicOverlay
import com.kmp.datacapture.barcode.selection.BarcodeSelectionListener
import com.kmp.datacapture.barcode.selection.BarcodeSelectionSession
import com.kmp.datacapture.barcode.selection.BarcodeSelectionSettings
import com.kmp.datacapture.barcode.selection.BarcodeSelectionTapSelection
import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.LogoStyle
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
class BarcodeSelectionScreenModel : BarcodeSelectionListener {

    private val _state = MutableStateFlow(BarcodeSelectionUiState())
    val state: StateFlow<BarcodeSelectionUiState> = _state.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [state] directly via Compose's `collectAsState`; iOS lacks a
    // built-in StateFlow → SwiftUI bridge, so the host calls
    // [observeState] from `BarcodeSelectionHostState.init` and stores the
    // returned [FlowCancellable] for the view's lifetime — no polling.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeState(onEach: (BarcodeSelectionUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    private val camera: Camera? =
        Camera.getDefaultCamera(BarcodeSelection.createRecommendedCameraSettings())?.also {
            dataCaptureContext.setFrameSource(it)
        }

    // The settings start with Tap-to-Select; the bottom-bar switch flips
    // this to the Aimer strategy at runtime via [selectMode]. We enable a
    // generous symbology set for the sample — in a real app enable only
    // what you need, as each symbology costs processing time.
    private val settings: BarcodeSelectionSettings =
        BarcodeSelectionSettings.barcodeSelectionSettings().also {
            it.selectionType = BarcodeSelectionTapSelection.tapSelection()
            it.enableSymbology(Symbology.EAN8, true)
            it.enableSymbology(Symbology.EAN13_UPCA, true)
            it.enableSymbology(Symbology.UPCE, true)
            it.enableSymbology(Symbology.QR, true)
            it.enableSymbology(Symbology.DATA_MATRIX, true)
            it.enableSymbology(Symbology.CODE39, true)
            it.enableSymbology(Symbology.CODE128, true)
        }

    // Public so the iOS host can wire the SDK's Swift bridge to it once.
    // The SDK internally bypasses K/N's per-callback ObjC retention via
    // a Swift-side listener implementation; this code path stays
    // platform-independent.
    val barcodeSelection: BarcodeSelection =
        BarcodeSelection.forContext(dataCaptureContext, settings).also {
            it.addListener(this)
        }

    fun setupDataCaptureView(view: DataCaptureView): DataCaptureView {
        view.logoStyle = LogoStyle.MINIMAL
        // The overlay renders the selection state on top of the preview;
        // recommended for visual feedback but not otherwise referenced.
        view.addOverlay(BarcodeSelectionBasicOverlay.withBarcodeSelectionForView(barcodeSelection, view))
        return view
    }

    // Camera state to restore when the sample is foregrounded again. Tap-to-
    // select freezes the camera by switching it OFF (see BarcodeSelection.
    // freezeCamera); a running preview is ON. Foregrounding must preserve
    // whichever state held before the sample went to the background — a frozen
    // preview stays frozen, a live one stays live — rather than blindly turning
    // the camera back ON. iOS gets this for free (SwiftUI onAppear/onDisappear
    // don't fire on app background), so restoring here aligns Android with it.
    private var stateBeforeStop: FrameSourceState = FrameSourceState.ON

    fun onEvent(event: BarcodeSelectionEvent) {
        when (event) {
            BarcodeSelectionEvent.STARTED -> {
                camera?.switchToDesiredState(stateBeforeStop)
                barcodeSelection.isEnabled = true
            }
            BarcodeSelectionEvent.STOPPED -> {
                stateBeforeStop = camera?.desiredState ?: FrameSourceState.ON
                barcodeSelection.isEnabled = false
                camera?.switchToDesiredState(FrameSourceState.OFF)
            }
        }
    }

    // Switch the selection strategy between Tap-to-Select and Aim-to-Select
    // by mutating the shared settings and re-applying them to the running
    // mode (no need to recreate BarcodeSelection).
    fun selectMode(mode: SelectionMode) {
        settings.selectionType = when (mode) {
            SelectionMode.TAP -> BarcodeSelectionTapSelection.tapSelection()
            SelectionMode.AIMER -> BarcodeSelectionAimerSelection.aimerSelection()
        }
        barcodeSelection.applySettings(settings)
        _state.value = _state.value.copy(selectionMode = mode)
    }

    override fun onSelectionUpdated(
        barcodeSelection: BarcodeSelection,
        session: BarcodeSelectionSession,
        frameData: FrameData?,
    ) {
        // Only surface a freshly selected barcode (the host shows it as a
        // transient toast). Re-selecting an already-selected barcode bumps
        // its count and shows up here again via newlySelectedBarcodes.
        val barcode = session.newlySelectedBarcodes.firstOrNull() ?: return
        _state.value = _state.value.copy(
            lastSelection = SelectedBarcode(
                data = barcode.data ?: "",
                symbologyName = barcode.symbology.name,
                count = session.getCount(barcode),
            ),
            selectionEventId = _state.value.selectionEventId + 1,
        )
    }

    override fun onSessionUpdated(
        barcodeSelection: BarcodeSelection,
        session: BarcodeSelectionSession,
        frameData: FrameData?,
    ) = Unit

    fun dispose() {
        barcodeSelection.isEnabled = false
        barcodeSelection.removeListener(this)
        dataCaptureContext.removeMode(barcodeSelection)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    companion object {
        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
