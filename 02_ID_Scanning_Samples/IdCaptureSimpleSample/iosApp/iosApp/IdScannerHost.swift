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
        .alert(
            alertTitle,
            isPresented: Binding(
                get: { !(host.state is IdScannerUiStateScanning) && host.state != nil },
                set: { isPresented in
                    if !isPresented { host.screenModel.onEvent(event: .dismissResult) }
                }
            ),
            presenting: alertMessage
        ) { _ in
            // The model's onEvent(.DISMISS_RESULT) is idempotent — SwiftUI
            // fires the alert's dismiss callback multiple times (Binding.set:,
            // Button action, animation re-evaluation), and the model guards
            // against re-arming while already Scanning.
            Button("Resume") { host.screenModel.onEvent(event: .dismissResult) }
        } message: { message in
            Text(message)
        }
    }

    // Map the captured / rejected state to the same title + body the native
    // sample shows; nil while scanning so the alert stays hidden.
    private var alertTitle: String {
        host.state is IdScannerUiStateRejected ? "Error Encountered" : "Recognized Document"
    }

    private var alertMessage: String? {
        switch host.state {
        case let captured as IdScannerUiStateCaptured: return captured.fields
        case let rejected as IdScannerUiStateRejected: return rejected.message
        default: return nil
        }
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
