/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.idcapturesimplesample

import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.LogoStyle
import com.kmp.datacapture.id.capture.IdCapture
import com.kmp.datacapture.id.capture.IdCaptureFeedback
import com.kmp.datacapture.id.capture.IdCaptureListener
import com.kmp.datacapture.id.capture.IdCaptureOverlay
import com.kmp.datacapture.id.capture.IdCaptureSettings
import com.kmp.datacapture.id.data.CapturedId
import com.kmp.datacapture.id.data.DateResult
import com.kmp.datacapture.id.data.DriverLicense
import com.kmp.datacapture.id.data.IdCaptureRegion
import com.kmp.datacapture.id.data.IdCard
import com.kmp.datacapture.id.data.Passport
import com.kmp.datacapture.id.scanner.FullDocumentScanner
import com.kmp.datacapture.id.scanner.IdCaptureScanner
import com.kmp.datacapture.id.scanner.RejectionReason
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
class IdScannerScreenModel : IdCaptureListener {

    private val _state = MutableStateFlow<IdScannerUiState>(IdScannerUiState.Scanning)
    val state: StateFlow<IdScannerUiState> = _state.asStateFlow()

    // Swift-friendly subscription on top of the StateFlow. Android consumers
    // collect [state] directly via Compose's `collectAsState`; iOS lacks a
    // built-in StateFlow → SwiftUI bridge, so the host calls [observeState]
    // from `IdScannerHostState.init` and stores the returned
    // [FlowCancellable] for the view's lifetime — no polling.
    private val observerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun observeState(onEach: (IdScannerUiState) -> Unit): FlowCancellable {
        val job = observerScope.launch {
            state.collect { onEach(it) }
        }
        return FlowCancellable(job)
    }

    val dataCaptureContext: DataCaptureContext = sharedContext

    private val camera: Camera? =
        Camera.getDefaultCamera(IdCapture.createRecommendedCameraSettings())?.also {
            dataCaptureContext.setFrameSource(it)
        }

    // Recognize national ID cards, driver's licenses and passports from any
    // region, reading the full document (front + AAMVA barcode where present).
    private val settings: IdCaptureSettings =
        IdCaptureSettings.idCaptureSettings().also {
            it.scanner = IdCaptureScanner(physicalDocument = FullDocumentScanner())
            it.acceptedDocuments = listOf(
                IdCard(IdCaptureRegion.ANY),
                DriverLicense(IdCaptureRegion.ANY),
                Passport(IdCaptureRegion.ANY),
            )
        }

    // Public so the iOS host can wire the SDK's Swift bridge to it once.
    // The SDK internally bypasses K/N's per-callback ObjC retention via a
    // Swift-side listener implementation; this code path stays
    // platform-independent.
    val idCapture: IdCapture =
        IdCapture.forContext(dataCaptureContext, settings).also {
            it.addListener(this)
            it.feedback = IdCaptureFeedback.defaultFeedback()
        }

    fun setupDataCaptureView(view: DataCaptureView): DataCaptureView {
        view.logoStyle = LogoStyle.MINIMAL
        // Document-localization feedback (matches the native sample). Not a
        // viewfinder — IdCapture has no rectangular target box.
        view.addOverlay(IdCaptureOverlay.withIdCaptureForView(idCapture, view))
        return view
    }

    fun onEvent(event: IdScannerEvent) {
        when (event) {
            IdScannerEvent.STARTED -> {
                camera?.switchToDesiredState(FrameSourceState.ON)
                idCapture.isEnabled = true
            }
            IdScannerEvent.STOPPED -> {
                idCapture.isEnabled = false
                camera?.switchToDesiredState(FrameSourceState.OFF)
            }
            IdScannerEvent.DISMISS_RESULT -> {
                if (_state.value !is IdScannerUiState.Scanning) {
                    _state.value = IdScannerUiState.Scanning
                    idCapture.reset()
                    idCapture.isEnabled = true
                }
            }
        }
    }

    override fun onIdCaptured(mode: IdCapture, id: CapturedId) {
        // Don't capture again while the result is shown.
        idCapture.isEnabled = false
        _state.value = IdScannerUiState.Captured(describe(id))
    }

    override fun onIdRejected(mode: IdCapture, id: CapturedId?, reason: RejectionReason) {
        idCapture.isEnabled = false
        _state.value = IdScannerUiState.Rejected(
            if (reason == RejectionReason.TIMEOUT) REJECT_TIMEOUT else REJECT_NOT_SUPPORTED,
        )
    }

    fun dispose() {
        idCapture.isEnabled = false
        idCapture.removeListener(this)
        dataCaptureContext.removeMode(idCapture)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    // Build the same field summary the native IdCaptureSimpleSample shows in
    // its alert. Only non-empty fields are appended.
    private fun describe(id: CapturedId): String = buildString {
        appendField("Full Name", id.fullName)
        appendField("Date of Birth", id.dateOfBirth?.let(::formatDate))
        appendField("Date of Expiry", id.dateOfExpiry?.let(::formatDate))
        appendField("Document Number", id.documentNumber)
        appendField("Nationality", id.nationality)
        appendField("Document Type", id.document?.documentType?.name)
    }.trimEnd()

    private fun StringBuilder.appendField(label: String, value: String?) {
        if (!value.isNullOrEmpty()) append("$label: $value\n")
    }

    companion object {
        private const val REJECT_NOT_SUPPORTED =
            "Document not supported. Try scanning another document."
        private const val REJECT_TIMEOUT =
            "Document capture failed. Make sure the document is well lit and free of glare. " +
                "Alternatively, try scanning another document."

        // KMP DateResult exposes only day/month/year (no platform Date), so
        // format here instead of relying on a localized DateFormat.
        private fun formatDate(d: DateResult): String {
            val month = d.month
            val day = d.day
            return if (month != null && day != null) {
                "${pad(d.year, 4)}-${pad(month, 2)}-${pad(day, 2)}"
            } else {
                d.year.toString()
            }
        }

        private fun pad(value: Int, width: Int): String = value.toString().padStart(width, '0')

        private val sharedContext: DataCaptureContext by lazy {
            DataCaptureContext.initialize(LICENSE_KEY)
        }
    }
}
