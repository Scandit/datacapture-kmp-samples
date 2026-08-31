/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Combine
import SwiftUI
import shared

// Root state holder: pins the shared screen model and its flow observer to
// the ContentView lifetime. Routing both through a single `@StateObject`
// keeps observer and actions pointing at the same model instance across
// SwiftUI body recomputations.
final class RestockingHostState: ObservableObject {
    let screenModel: RestockingScreenModel
    // Held here so the BarcodePickView survives the Pick <-> Result view
    // identity swap in `scannerFlow` — see PickViewStore (SDC-33498).
    let pickViewStore = PickViewStore()
    @Published var state: RestockingUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = RestockingScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        // `sink` with `[weak self]` — `assign(to:\.state, on: self)` retains
        // self while self holds the cancellable, so deinit (and thus
        // screenModel.dispose()) would never run and the BarcodePick mode +
        // listeners + coroutine scope would leak for the app's lifetime.
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] value in self?.state = value }
    }

    deinit { screenModel.dispose() }
}

// iOS root view. Mirrors `RestockingApp` on Android — a landing page
// (HomeView) that handles the camera-permission flow, then the pick/result
// flow driven by the shared screen model's StateFlow.
struct ContentView: View {
    @StateObject private var host = RestockingHostState()
    @State private var showScanner = false

    var body: some View {
        NavigationStack {
            HomeView(onStartScan: { showScanner = true })
                .navigationDestination(isPresented: $showScanner) {
                    scannerFlow
                        .navigationBarBackButtonHidden(true)
                }
        }
    }

    @ViewBuilder
    private var scannerFlow: some View {
        let current = host.state
        if let result = current as? RestockingUiStateResult {
            ResultView(
                items: (result.items as? [ListItem]) ?? [],
                onContinueScanning: {
                    host.screenModel.onEvent(event: RestockingEventContinueScanning.shared)
                },
                // Finish pickup: clear the session and return to the landing
                // page; starting a new scan begins a fresh picking session.
                onFinishPickup: {
                    // Tear the pick view down on the event that ends the
                    // session, so it is not left paused and bound to a mode
                    // the model has already detached from.
                    host.pickViewStore.tearDown(screenModel: host.screenModel)
                    host.screenModel.onEvent(event: RestockingEventFinishPickup.shared)
                    showScanner = false
                }
            )
        } else {
            // Pick (or state not yet emitted — the initial value is Pick).
            PickView(screenModel: host.screenModel, store: host.pickViewStore)
        }
    }
}
