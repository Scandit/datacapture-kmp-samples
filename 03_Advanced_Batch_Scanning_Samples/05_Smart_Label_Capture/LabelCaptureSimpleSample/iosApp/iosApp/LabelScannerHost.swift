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
// to this view's lifetime. SwiftUI re-runs `LabelScannerHost.init()` on
// every body recomputation, so a plain stored property would construct a
// fresh `LabelScannerScreenModel` each time — leaving any `@StateObject`
// observer attached to the original model's flow while UI actions hit the
// new one. Routing both through a single `@StateObject` keeps observer
// and actions pointing at the same instance.
//
// The holder re-publishes the inner FlowObserver's value: SwiftUI tracks
// dependencies on the directly-observed `@StateObject`, so nested
// `ObservableObject`s need an explicit relay through @Published / a
// Combine subscription for the view to re-render on state changes.
final class LabelScannerHostState: ObservableObject {
    let screenModel: LabelScannerScreenModel
    @Published var state: LabelScannerUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = LabelScannerScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
        // Capture is driven by the validation-flow overlay's listener, wired
        // when the screen model builds the overlays in setupDataCaptureView.
    }

    deinit {
        // Releases the SDK resources the model owns (overlays, mode, camera).
        screenModel.dispose()
    }
}

struct LabelScannerHost: View {
    @StateObject private var host = LabelScannerHostState()

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
            "Captured Label",
            isPresented: Binding(
                get: { host.state is LabelScannerUiStateScanned },
                set: { isPresented in
                    if !isPresented { host.screenModel.onEvent(event: .dismissResult) }
                }
            ),
            presenting: host.state as? LabelScannerUiStateScanned
        ) { _ in
            // The model's onEvent(.DISMISS_RESULT) is idempotent —
            // SwiftUI fires the alert's dismiss callback multiple times
            // (Binding.set:, Button action, animation re-evaluation),
            // and the model guards against repeat `isEnabled=true` calls.
            Button("Continue") { host.screenModel.onEvent(event: .dismissResult) }
        } message: { scanned in
            // `fields` bridges from Kotlin as [LabelFieldResult]; render one
            // "name: value" row per captured field.
            Text(
                scanned.fields
                    .map { "\($0.name): \($0.value)" }
                    .joined(separator: "\n")
            )
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
