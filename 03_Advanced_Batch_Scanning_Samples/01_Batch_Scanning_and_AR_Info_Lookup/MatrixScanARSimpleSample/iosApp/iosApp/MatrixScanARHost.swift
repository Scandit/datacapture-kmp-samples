/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import shared

// Scanner host for a single ArMode: state holder + full-screen camera preview view + the
// UIViewRepresentable bridge that embeds the SDK's `UIView` into SwiftUI. The shared screen
// model owns all SDK wiring; this host only embeds the platform-native BarcodeArView.

// State-owned holder that pins the shared screen model to this view's lifetime. SwiftUI
// re-runs `MatrixScanARHost.init()` on every body recomputation, so a plain stored property
// would construct a fresh model each time.
final class MatrixScanARHostState: ObservableObject {
    let screenModel: MatrixScanARScreenModel
    let barcodeArView: BarcodeArView

    init(mode: ArMode) {
        let model = MatrixScanARScreenModel()
        model.setMode(mode: mode)
        self.screenModel = model
        self.barcodeArView = model.registerView(
            view: BarcodeArView(barcodeAr: model.barcodeAr, settings: model.buildViewSettings())
        )
    }

    deinit {
        screenModel.dispose()
    }
}

struct MatrixScanARHost: View {
    let mode: ArMode
    let onExit: () -> Void
    @StateObject private var host: MatrixScanARHostState

    init(mode: ArMode, onExit: @escaping () -> Void) {
        self.mode = mode
        self.onExit = onExit
        _host = StateObject(wrappedValue: MatrixScanARHostState(mode: mode))
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            BarcodeArViewRepresentable { host.barcodeArView.toUIView() }
                .edgesIgnoringSafeArea(.all)
                .onAppear { host.screenModel.onResume() }
                .onDisappear { host.screenModel.onPause() }

            ReturnButton(onClick: onExit)
                .padding(.leading, 24)
                .padding(.bottom, 24)
        }
    }
}

// Bottom-left "Return" affordance mirroring the native Android sample's circular grey
// ic_return button + small white label beneath it. No native iOS reference exists for this
// sample, so the SF Symbol chevron.left stands in for the native ImageButton icon.
private struct ReturnButton: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 2) {
                ZStack {
                    Circle()
                        .fill(Color.scanditReturnButtonGrey)
                        .frame(width: 56, height: 56)
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                }
                Text("Return")
                    .font(.system(size: 10))
                    .foregroundColor(.white)
            }
        }
    }
}

struct BarcodeArViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }

    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}
