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

    var body: some View {
        // No NavigationView here — the host (ContentView) provides the
        // navigation stack + "MatrixScan Count" title bar, so device back
        // returns to Home.
        BarcodeCountViewRepresentable(
            screenModel: screenModel,
            onListTap: onListTap,
            onExitTap: onExitTap
        )
        .edgesIgnoringSafeArea(.bottom)
        .navigationTitle("MatrixScan Count")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.black, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        // This screen deliberately does not touch the camera. `ContentView`
        // switches the frame source per screen, so there is exactly one place
        // that starts and stops scanning. What this screen does own is the
        // view's own scanning lifecycle — see the coordinator below.
    }
}

private struct BarcodeCountViewRepresentable: UIViewRepresentable {
    let screenModel: MatrixScanCountScreenModel
    let onListTap: () -> Void
    let onExitTap: () -> Void

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
        return view.toUIView()
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        // No dynamic props to sync; the view binds to the mode's settings.
    }

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.view?.onPause()
        // Drop the reference with the pause. The coordinator's app-lifecycle
        // observers can outlive this call, and resuming a view that has already
        // been paused and detached would leave it prepared for scanning with
        // nothing left to pause it.
        coordinator.view = nil
    }

    // Kotlin interface defaults do not bridge as optional Swift protocol
    // requirements, so every method on `BarcodeCountViewUiListener` has to be
    // implemented here even when this screen ignores it (SDC-31092).
    final class Coordinator: NSObject, BarcodeCountViewUiListener {
        let onListTap: () -> Void
        let onExitTap: () -> Void
        var view: BarcodeCountView?

        private var observers: [NSObjectProtocol] = []

        init(onListTap: @escaping () -> Void, onExitTap: @escaping () -> Void) {
            self.onListTap = onListTap
            self.onExitTap = onExitTap
            super.init()
            // `BarcodeCountView` has its own resume/pause lifecycle, separate
            // from the frame source: pair them or the view stops scanning when
            // the app is backgrounded and nothing restarts it, leaving a frozen
            // preview on return. Backgrounding does not unmount this
            // representable, so there is no SwiftUI callback to hang them on —
            // observe the app notifications instead. These fire app-wide, so
            // they also run while the results list is layered over this screen;
            // that is harmless, because the camera is off there and both calls
            // are no-ops when the view is already in that state.
            let center = NotificationCenter.default
            observers.append(
                center.addObserver(
                    forName: UIApplication.didBecomeActiveNotification,
                    object: nil,
                    queue: .main
                ) { [weak self] _ in self?.view?.onResume() }
            )
            observers.append(
                center.addObserver(
                    forName: UIApplication.willResignActiveNotification,
                    object: nil,
                    queue: .main
                ) { [weak self] _ in self?.view?.onPause() }
            )
        }

        deinit {
            observers.forEach(NotificationCenter.default.removeObserver)
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
