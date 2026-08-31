/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Combine
import SwiftUI
import shared

// Scanner host: state holder + full-screen camera preview view + the
// UIViewRepresentable bridge that embeds the SDK's `UIView` into SwiftUI.

private extension DataCaptureView {
    var hostView: UIView { toUIView() }
}

// State-owned holder that pins the shared screen model + its flow observer
// to this view's lifetime. SwiftUI re-runs `IdScannerHost.init()` on every
// body recomputation, so a plain stored property would construct a fresh
// `IdScannerScreenModel` each time — leaving any `@StateObject` observer
// attached to the original model's flow while UI actions hit the new one.
// Routing both through a single `@StateObject` keeps observer and actions
// pointing at the same instance.
//
// The holder re-publishes the inner FlowObserver's value: SwiftUI tracks
// dependencies on the directly-observed `@StateObject`, so nested
// `ObservableObject`s need an explicit relay through @Published / a
// Combine subscription for the view to re-render on state changes.
final class IdScannerHostState: ObservableObject {
    let screenModel: IdScannerScreenModel
    @Published var state: IdScannerUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = IdScannerScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
        // No bridge instantiation here — the SDK's Swift bridge is
        // auto-attached on the first `idCapture.addListener(…)` call (which
        // the screen model already did in its init).
    }

    deinit {
        screenModel.dispose()
    }
}

struct IdScannerHost: View {
    @StateObject private var host = IdScannerHostState()

    var body: some View {
        ZStack {
            DataCaptureViewRepresentable {
                // DataCaptureView takes no host parameter on iOS — its
                // internal SDCDataCaptureView starts at zero frame and
                // follows Auto Layout once embedded.
                let dcView = host.screenModel.setupDataCaptureView(
                    view: DataCaptureView(dataCaptureContext: host.screenModel.dataCaptureContext)
                )
                return dcView.hostView
            }
            .edgesIgnoringSafeArea(.all)
        }
        .onAppear { host.screenModel.onEvent(event: .started) }
        .onDisappear { host.screenModel.onEvent(event: .stopped) }
        // Rich verification result — bottom sheet mirroring the native sample.
        .sheet(
            isPresented: Binding(
                get: { host.state is IdScannerUiStateResult },
                set: { isPresented in
                    if !isPresented { host.screenModel.onEvent(event: .dismissResult) }
                }
            )
        ) {
            if let result = host.state as? IdScannerUiStateResult {
                ResultSheetView(result: result) {
                    host.screenModel.onEvent(event: .dismissResult)
                }
            }
        }
        // Non-verification rejection (unsupported type, timeout, …) — plain
        // message, matching the native sample's separate rejected path.
        .alert(
            "Error Encountered",
            isPresented: Binding(
                get: { host.state is IdScannerUiStateRejected },
                set: { isPresented in
                    if !isPresented { host.screenModel.onEvent(event: .dismissResult) }
                }
            ),
            presenting: (host.state as? IdScannerUiStateRejected)?.message
        ) { _ in
            // onEvent(.DISMISS_RESULT) is idempotent — SwiftUI fires the
            // dismiss callback multiple times; the model guards re-arming.
            Button("Resume") { host.screenModel.onEvent(event: .dismissResult) }
        } message: { message in
            Text(message)
        }
    }
}

// The rich result screen: captured image, per-check verification tiles, then
// the full field list — the SwiftUI counterpart of the native sample's
// ResultBottomSheetFragment.
struct ResultSheetView: View {
    let result: IdScannerUiStateResult
    let onResume: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(result.title)
                    .font(.title2).bold()

                if let image = result.image {
                    Image(uiImage: image.toUIImage())
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: 220)
                }

                ForEach(Array(result.checks.enumerated()), id: \.offset) { _, check in
                    VerificationTileView(check: check)
                }

                Divider()

                ForEach(Array(result.fields.enumerated()), id: \.offset) { _, field in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(field.label)
                            .font(.caption)
                            .foregroundColor(.scanditGrey)
                        Text(field.value)
                            .font(.body)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button("Resume", action: onResume)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .padding(24)
        }
    }
}

struct VerificationTileView: View {
    let check: IdScannerUiStateVerificationCheck

    var body: some View {
        let color: SwiftUI.Color = check.passed ? .scanditGreen : .scanditRed
        HStack(spacing: 12) {
            Image(systemName: check.passed ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundColor(color)
            Text(check.label)
                .font(.subheadline)
            Spacer()
        }
        .padding(12)
        .background(color.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct DataCaptureViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }

    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}
