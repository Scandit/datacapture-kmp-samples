/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit
import shared

// Find phase: a full-screen BarcodeFindView. The view owns its own chrome
// (carousel, hints, finish button); tapping the built-in finish button fires
// BarcodeFindViewUiListener.onFinishButtonTapped, which returns to Search with
// the scanned items intact. Mirrors FindScanScreen on Android.

// Forwards the BarcodeFindView finish-button tap to the exit handler.
private final class FindUiListenerImpl: BarcodeFindViewUiListener {
    let onFinish: () -> Void
    init(onFinish: @escaping () -> Void) { self.onFinish = onFinish }
    func onFinishButtonTapped(foundItems: Set<BarcodeFindItem>) {
        onFinish()
    }
}

// UIViewRepresentable that embeds the KMP BarcodeFindView's container UIView.
private struct BarcodeFindViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }
    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}

// Implements BarcodeFindViewHandle so the shared model can drive lifecycle.
private final class BarcodeFindHandleImpl: BarcodeFindViewHandle {
    weak var view: BarcodeFindView?
    func onResume() { view?.onResume() }
    func onPause() { view?.onPause() }
    func startSearching() { view?.startSearching() }
    func stopSearching() { view?.stopSearching() }
}

struct FindView: View {
    let screenModel: SearchAndFindScreenModel
    let onExit: () -> Void

    @State private var barcodeFindView: BarcodeFindView?
    @State private var handle = BarcodeFindHandleImpl()
    @State private var uiListener: FindUiListenerImpl?

    var body: some View {
        ZStack {
            if let bfv = barcodeFindView {
                BarcodeFindViewRepresentable {
                    bfv.toUIView()
                }
                .ignoresSafeArea()
            }
        }
        .onAppear {
            guard let barcodeFind = screenModel.barcodeFindOrNull() else {
                // Guard: startFind() must have run before this view appears.
                return
            }
            if barcodeFindView == nil {
                let view = BarcodeFindView(
                    barcodeFind: barcodeFind,
                    settings: screenModel.barcodeFindViewSettings
                )
                barcodeFindView = view
                handle.view = view
                let listener = FindUiListenerImpl(onFinish: onExit)
                uiListener = listener
                view.uiListener = listener
                screenModel.registerBarcodeFindView(handle: handle)
            }
            screenModel.onFindResumed()
        }
        .onDisappear {
            screenModel.onFindPaused()
            screenModel.disposeFind()
        }
    }
}
