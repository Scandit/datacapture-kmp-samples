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
// to this view's lifetime. SwiftUI re-runs `BarcodeSelectionHost.init()` on
// every body recomputation, so a plain stored property would construct a
// fresh `BarcodeSelectionScreenModel` each time — leaving any `@StateObject`
// observer attached to the original model's flow while UI actions hit the
// new one. Routing both through a single `@StateObject` keeps observer
// and actions pointing at the same instance.
//
// The holder re-publishes the inner FlowObserver's value: SwiftUI tracks
// dependencies on the directly-observed `@StateObject`, so nested
// `ObservableObject`s need an explicit relay through @Published / a
// Combine subscription for the view to re-render on state changes.
final class BarcodeSelectionHostState: ObservableObject {
    let screenModel: BarcodeSelectionScreenModel
    @Published var state: BarcodeSelectionUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = BarcodeSelectionScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
        // No bridge instantiation here — the SDK's Swift bridge is
        // auto-attached on the first `barcodeSelection.addListener(…)`
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

struct BarcodeSelectionHost: View {
    @StateObject private var host = BarcodeSelectionHostState()

    // Bottom segmented control state, mirrored to the shared model's
    // selection strategy. 0 = Tap-to-Select, 1 = Aim-to-Select.
    @State private var modeIndex = 0

    // Transient toast text for the most recent selection; cleared after a
    // short delay so it behaves like the Flutter sample's snackbar.
    @State private var toast: String?

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                DataCaptureViewRepresentable {
                    // SDC-31709: DataCaptureView takes no host parameter on
                    // iOS — its internal SDCDataCaptureView starts at zero
                    // frame and follows Auto Layout once embedded.
                    let dcView = host.screenModel.setupDataCaptureView(
                        view: DataCaptureView(dataCaptureContext: host.screenModel.dataCaptureContext)
                    )
                    return dcView.hostView
                }
                .edgesIgnoringSafeArea(.top)

                Picker("Selection mode", selection: $modeIndex) {
                    Text("Tap to Select").tag(0)
                    Text("Aim to Select").tag(1)
                }
                .pickerStyle(.segmented)
                .padding()
                .background(Color.scanditLightGrey)
                .onChange(of: modeIndex) { index in
                    host.screenModel.selectMode(mode: index == 0 ? .tap : .aimer)
                }
            }

            if let toast {
                Text(toast)
                    .padding()
                    .background(Color.scanditBlack.opacity(0.85))
                    .foregroundColor(.white)
                    .cornerRadius(8)
                    .padding(.bottom, 80)
            }
        }
        // Fire the toast on each fresh selection. The shared model bumps
        // `selectionEventId` for every newly selected barcode (including a
        // re-select that only changes the count), so keying on it is what
        // makes repeats show.
        .onReceive(host.$state) { state in
            guard let state, state.selectionEventId > 0,
                let selection = state.lastSelection
            else { return }
            toast = "\(selection.symbologyName): \(selection.data)\nTimes: \(selection.count)"
            let shown = state.selectionEventId
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                if host.state?.selectionEventId == shown { toast = nil }
            }
        }
        .onAppear { host.screenModel.onEvent(event: .started) }
        .onDisappear { host.screenModel.onEvent(event: .stopped) }
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
