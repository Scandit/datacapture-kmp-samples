/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit
import shared

// The MatrixScan Count scanning screen. Hosts the SDK's platform
// `BarcodeCountView` (not the Compose Multiplatform wrapper — this is plain
// SwiftUI) bound to the screen model's own long-lived `barcodeCount`
// instance, so aggregated results survive navigating to Results and back.
struct ScannerView: View {
    let screenModel: MatrixScanCountScreenModel
    let onListTap: () -> Void
    let onExitTap: () -> Void
    // Exposes the underlying BarcodeCountView so the host (ContentView) can
    // pause/resume it around the Results full-screen-cover presentation,
    // since SwiftUI keeps this view mounted underneath the cover.
    var onViewCreated: ((BarcodeCountView) -> Void)? = nil

    var body: some View {
        // No NavigationView here — the host (ContentView) provides the
        // navigation stack + "MatrixScan Count" title bar, so device back
        // returns to Home.
        BarcodeCountViewRepresentable(
            screenModel: screenModel,
            onListTap: onListTap,
            onExitTap: onExitTap,
            onViewCreated: onViewCreated
        )
        .edgesIgnoringSafeArea(.bottom)
        .navigationTitle("MatrixScan Count")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { screenModel.resumeFrameSource() }
        .onDisappear { screenModel.pauseFrameSource() }
    }
}

private struct BarcodeCountViewRepresentable: UIViewRepresentable {
    let screenModel: MatrixScanCountScreenModel
    let onListTap: () -> Void
    let onExitTap: () -> Void
    let onViewCreated: ((BarcodeCountView) -> Void)?

    func makeCoordinator() -> Coordinator {
        Coordinator(onListTap: onListTap, onExitTap: onExitTap)
    }

    func makeUIView(context: Context) -> UIView {
        let view = BarcodeCountView(barcodeCount: screenModel.barcodeCount, style: .icon)
        view.shouldShowListButton = true
        view.shouldShowExitButton = true
        view.addListener(listener: context.coordinator)
        context.coordinator.view = view
        view.onResume()
        onViewCreated?(view)
        return view.toUIView()
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        // No dynamic props to sync; the view binds to the mode's settings.
    }

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.view?.onPause()
    }

    // Kotlin interface defaults do not bridge as optional Swift protocol
    // requirements, so every method on `BarcodeCountViewUiListener` has to be
    // implemented here even when this screen ignores it (SDC-31092).
    final class Coordinator: NSObject, BarcodeCountViewUiListener {
        let onListTap: () -> Void
        let onExitTap: () -> Void
        var view: BarcodeCountView?

        init(onListTap: @escaping () -> Void, onExitTap: @escaping () -> Void) {
            self.onListTap = onListTap
            self.onExitTap = onExitTap
        }

        func onListButtonTapped(view: BarcodeCountView, snapshot: BarcodeCountSessionSnapshot?) {
            onListTap()
        }

        func onExitButtonTapped(view: BarcodeCountView, snapshot: BarcodeCountSessionSnapshot?) {
            onExitTap()
        }

        func onSingleScanButtonTapped(view: BarcodeCountView) {
            // Not used — the single-scan button is hidden by default.
        }
    }
}
