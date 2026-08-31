/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.usdlverificationsample

import com.kmp.datacapture.core.capture.DataCaptureContext
import com.kmp.datacapture.core.source.Camera
import com.kmp.datacapture.core.source.FrameSourceState
import com.kmp.datacapture.core.ui.DataCaptureView
import com.kmp.datacapture.core.ui.CapturedImage
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
import com.kmp.datacapture.id.data.IdImageType
import com.kmp.datacapture.id.data.IdSide
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

    // Scan US driver's licenses, reading the full document: front + the AAMVA
    // barcode on the back. The reject rules below run the on-device AAMVA
    // barcode verification (backed by the id-aamva-barcode-verification
    // module): documents with a forged barcode, inconsistent front/back data,
    // or that are expired are rejected. Mirrors the native USDLVerificationSample.
    private val settings: IdCaptureSettings =
        IdCaptureSettings.idCaptureSettings().also {
            it.scanner = IdCaptureScanner(physicalDocument = FullDocumentScanner())
            it.acceptedDocuments = listOf(
                DriverLicense(IdCaptureRegion.US),
            )
            it.rejectForgedAamvaBarcodes = true
            it.rejectInconsistentData = true
            it.rejectExpiredIds = true
            // Surface the captured images on the result — the rich result
            // screen shows the face crop / cropped document. Off by default,
            // so the SDK won't populate CapturedId.images without this.
            it.setShouldPassImageTypeToResult(IdImageType.FACE, true)
            it.setShouldPassImageTypeToResult(IdImageType.CROPPED_DOCUMENT, true)
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
                    // Resume scanning: camera back on (last, per mount ordering),
                    // then re-enable capture.
                    camera?.switchToDesiredState(FrameSourceState.ON)
                    idCapture.isEnabled = true
                }
            }
        }
    }

    override fun onIdCaptured(mode: IdCapture, id: CapturedId) {
        // Stop scanning while the result is shown — disable capture and turn
        // the camera off (matches the native sample, which pauses the scan
        // screen). Resumed in DISMISS_RESULT.
        idCapture.isEnabled = false
        camera?.switchToDesiredState(FrameSourceState.OFF)
        // Reaching onIdCaptured means every enabled reject rule passed, so all
        // three verification checks succeeded — three green tiles.
        _state.value = IdScannerUiState.Result(
            title = "Recognized Document",
            image = resultImage(id),
            checks = listOf(
                IdScannerUiState.VerificationCheck(CHECK_NOT_EXPIRED, passed = true),
                IdScannerUiState.VerificationCheck(CHECK_DATA_CONSISTENT, passed = true),
                IdScannerUiState.VerificationCheck(CHECK_BARCODE_AUTHENTIC, passed = true),
            ),
            fields = describe(id),
        )
    }

    override fun onIdRejected(mode: IdCapture, id: CapturedId?, reason: RejectionReason) {
        idCapture.isEnabled = false
        camera?.switchToDesiredState(FrameSourceState.OFF)
        val failedCheck = failedVerificationCheck(reason)
        // Verification-rule rejections (forged barcode, inconsistent data,
        // expired) carry a CapturedId and route to the rich result screen with
        // a single red tile — mirroring the native sample. Everything else
        // (unsupported type, bad format, timeout) is a plain message.
        if (failedCheck != null && id != null) {
            _state.value = IdScannerUiState.Result(
                title = "Document Rejected",
                // On a data-consistency rejection the front review image is the
                // relevant visual; otherwise the captured document crop.
                image = id.verificationResult?.dataConsistency?.frontReviewImage ?: resultImage(id),
                checks = listOf(
                    IdScannerUiState.VerificationCheck(failedCheck, passed = false),
                ),
                fields = describe(id),
            )
        } else {
            _state.value = IdScannerUiState.Rejected(rejectionMessage(reason))
        }
    }

    fun dispose() {
        idCapture.isEnabled = false
        idCapture.removeListener(this)
        dataCaptureContext.removeMode(idCapture)
        camera?.switchToDesiredState(FrameSourceState.OFF)
        observerScope.cancel()
    }

    // Build the full field list the native USDLVerificationSample shows on its
    // result screen, in the same order. Every field is included; missing
    // values render as [IdScannerUiState.EMPTY_VALUE].
    private fun describe(id: CapturedId): List<IdScannerUiState.Field> = listOf(
        field("Full Name", id.fullName),
        field("Sex", id.sexType.name),
        field("Nationality", id.nationality),
        field("Date of Birth", id.dateOfBirth?.let(::formatDate)),
        field("Address", id.address),
        field("Document Number", id.documentNumber),
        field("Document Additional Number", id.documentAdditionalNumber),
        field("Date of Expiry", id.dateOfExpiry?.let(::formatDate)),
        field("Issuing Country", id.issuingCountry.name),
        field("Date of Issue", id.dateOfIssue?.let(::formatDate)),
    )

    // Pick the captured image for the result screen: the cropped front of the
    // document first, then the face crop. Both require the matching
    // setShouldPassImageTypeToResult flag in [settings].
    private fun resultImage(id: CapturedId): CapturedImage? =
        id.images.croppedDocument(IdSide.FRONT) ?: id.images.face

    private fun field(label: String, value: String?): IdScannerUiState.Field =
        IdScannerUiState.Field(label, value?.takeUnless { it.isEmpty() } ?: IdScannerUiState.EMPTY_VALUE)

    // Map a verification-rule rejection to its red-tile label. Returns null for
    // non-verification rejections (unsupported type, bad format, timeout, …),
    // which take the plain-message path instead.
    private fun failedVerificationCheck(reason: RejectionReason): String? = when (reason) {
        RejectionReason.DOCUMENT_EXPIRED -> CHECK_EXPIRED
        RejectionReason.INCONSISTENT_DATA -> CHECK_DATA_INCONSISTENT
        RejectionReason.FORGED_AAMVA_BARCODE -> CHECK_BARCODE_FORGED
        else -> null
    }

    // Map a non-verification rejection reason to a user-facing message. The
    // verification reasons are handled by [failedVerificationCheck] / the rich
    // result screen; this covers the remaining reasons. Mirrors the native
    // sample's separate rejected-document path.
    private fun rejectionMessage(reason: RejectionReason): String = when (reason) {
        RejectionReason.TIMEOUT -> REJECT_TIMEOUT
        else -> REJECT_NOT_SUPPORTED
    }

    companion object {
        // Verification-tile labels — same wording as the native sample's
        // res/values/strings.xml (scanning_dl_*).
        private const val CHECK_NOT_EXPIRED = "Document has not expired."
        private const val CHECK_EXPIRED = "Document has expired."
        private const val CHECK_DATA_CONSISTENT = "Information on front and back matches."
        private const val CHECK_DATA_INCONSISTENT = "Information on front and back does not match."
        private const val CHECK_BARCODE_AUTHENTIC = "Document barcode is authentic."
        private const val CHECK_BARCODE_FORGED = "Document barcode is forged."

        private const val REJECT_NOT_SUPPORTED =
            "Document not supported. Try scanning another document"
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
