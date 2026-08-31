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
// to this view's lifetime. SwiftUI re-runs `BarcodeScannerHost.init()` on
// every body recomputation, so a plain stored property would construct a
// fresh `BarcodeScannerScreenModel` each time — leaving any `@StateObject`
// observer attached to the original model's flow while UI actions hit the
// new one. Routing both through a single `@StateObject` keeps observer
// and actions pointing at the same instance.
//
// The holder re-publishes the inner FlowObserver's value: SwiftUI tracks
// dependencies on the directly-observed `@StateObject`, so nested
// `ObservableObject`s need an explicit relay through @Published / a
// Combine subscription for the view to re-render on state changes.
final class BarcodeScannerHostState: ObservableObject {
    let screenModel: BarcodeScannerScreenModel
    @Published var state: BarcodeScannerUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = BarcodeScannerScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
        // No bridge instantiation here — the SDK's Swift bridge is
        // auto-attached on the first `barcodeCapture.addListener(…)`
        // call (which the screen model already did in its init). See
        // ScanditKmpPackage/Sources/ScanditKmpBarcodeBridgeObjC for
        // the dyld-load constructor that wires it up.
    }

    deinit {
        // captureBridge deinit detaches the listener via Swift ARC; explicit
        // for clarity.
        screenModel.dispose()
    }
}

struct BarcodeScannerHost: View {
    @StateObject private var host = BarcodeScannerHostState()

    var body: some View {
        ZStack {
            DataCaptureViewRepresentable {
                // SDC-31709: DataCaptureView takes no host parameter on
                // iOS — its internal SDCDataCaptureView starts at zero
                // frame and follows Auto Layout once embedded.
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
            "Scanned",
            isPresented: Binding(
                get: { host.state is BarcodeScannerUiStateScanned },
                set: { isPresented in
                    if !isPresented { host.screenModel.onEvent(event: .dismissResult) }
                }
            ),
            presenting: host.state as? BarcodeScannerUiStateScanned
        ) { _ in
            // The model's onEvent(.DISMISS_RESULT) is idempotent —
            // SwiftUI fires the alert's dismiss callback multiple times
            // (Binding.set:, Button action, animation re-evaluation),
            // and the model guards against repeat `isEnabled=true` calls.
            Button("OK") { host.screenModel.onEvent(event: .dismissResult) }
        } message: { scanned in
            Text("\(scanned.data)\n(\(scanned.symbologyName))")
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
