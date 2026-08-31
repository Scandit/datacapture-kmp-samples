/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit
import shared

// Pick phase: a full-screen BarcodePickView. The view owns its own chrome
// (highlights, hints, finish button); tapping the built-in finish button
// fires `RestockingScreenModel.onFinishButtonTapped` (the screen model
// implements BarcodePickViewUiListener directly), moving to the Result
// screen. Mirrors PickScreen on Android and FindView in the sibling
// SearchAndFindSample.

// UIViewRepresentable that embeds the KMP BarcodePickView's container UIView.
private struct BarcodePickViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }
    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}

// Implements BarcodePickViewHandle so the shared model can drive lifecycle.
private final class BarcodePickHandleImpl: BarcodePickViewHandle {
    weak var view: BarcodePickView?
    func start() { view?.start() }
    func onResume() { view?.onResume() }
    func onPause() { view?.onPause() }
    func stop() { view?.stop() }
}

// Owns the BarcodePickView for the whole pick/result flow.
//
// This deliberately does NOT live in PickView's @State. The root view swaps
// between ResultView and PickView with an `if`/`else` (ContentView.scannerFlow),
// and SwiftUI gives the two branches distinct view identities — so returning
// from the result list via "Continue scanning" hands PickView a brand-new
// @State. Held there, `barcodePickView` was nil again on every return, so
// onAppear built a SECOND BarcodePickView on the still-running BarcodePick
// mode while the first one was only paused, never stopped. Two live views on
// one mode abort inside the SDK's data-capture-context queue (SDC-33498).
//
// Android has no equivalent bug: PickScreen's DisposableEffect stops and
// unregisters its view when the composable leaves composition, so only one
// view is ever bound to the mode there.
final class PickViewStore {
    private var view: BarcodePickView?
    private let handle = BarcodePickHandleImpl()

    // The mode the current view is bound to. "Finish pickup" builds a fresh
    // BarcodePick, so a view held from the previous session must be torn down
    // rather than reused against a mode it is no longer attached to.
    private var boundMode: BarcodePick?

    func view(for screenModel: RestockingScreenModel) -> BarcodePickView {
        if let existing = view, boundMode === screenModel.barcodePick {
            // onDisappear paused it; start() is what resumes picking.
            // onResume() is inert on iOS, so it cannot do this.
            existing.start()
            return existing
        }

        // Safety net: a superseded view should already have been torn down by
        // tearDown() on "Finish pickup". Catches any path that did not.
        view?.stop()

        let created = BarcodePickView(
            barcodePick: screenModel.barcodePick,
            settings: screenModel.barcodePickViewSettings
        )
        created.uiListener = screenModel
        created.addActionListener(listener: screenModel)
        view = created
        boundMode = screenModel.barcodePick
        handle.view = created
        screenModel.registerBarcodePickView(handle: handle)
        // start() is the final mount step.
        created.start()
        return created
    }

    // Terminal teardown, driven by "Finish pickup" (ContentView.scannerFlow).
    // That event detaches the listeners from the old BarcodePick and replaces
    // the mode, so the view bound to it is done — stop it here rather than
    // waiting for the next mount to notice the mode changed. Without this the
    // superseded view stays retained and merely paused, still holding the
    // screen model as uiListener and action listener; if the user never scans
    // again it is never stopped at all. Mirrors PickScreen's DisposableEffect
    // on Android.
    func tearDown(screenModel: RestockingScreenModel) {
        view?.stop()
        view = nil
        boundMode = nil
        handle.view = nil
        screenModel.unregisterBarcodePickView()
    }
}

struct PickView: View {
    let screenModel: RestockingScreenModel

    let store: PickViewStore

    @State private var barcodePickView: BarcodePickView?

    var body: some View {
        ZStack {
            if let bpv = barcodePickView {
                BarcodePickViewRepresentable {
                    bpv.toUIView()
                }
                .ignoresSafeArea()
            }
        }
        .onAppear {
            // The store hands back the one view for this picking session,
            // building it only on first mount or after "Finish pickup"
            // replaced the mode.
            barcodePickView = store.view(for: screenModel)
            screenModel.onPickResumed()
        }
        .onDisappear {
            // Pause when the screen is only temporarily left, so scanning can be
            // resumed later ("Continue scanning" comes back to this same session).
            // stop() would tear the view down for good; it is called once, from
            // RestockingScreenModel.dispose(), when the sample shuts down.
            screenModel.onPickPaused()
            barcodePickView?.pause()
        }
    }
}
