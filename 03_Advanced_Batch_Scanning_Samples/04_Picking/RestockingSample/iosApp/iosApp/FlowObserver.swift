/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Foundation
import shared

// Event-driven bridge from `RestockingScreenModel.state` (Kotlin StateFlow)
// to SwiftUI. The shared module exposes `observeState` — a callback-based
// subscription backed by a coroutine collector on `Dispatchers.Main` — so
// this side just relays values into a `@Published` property. No Timer
// polling, no spurious re-renders: the publisher fires only when the
// upstream StateFlow emits.
final class FlowObserver: ObservableObject {
    @Published var value: RestockingUiState?

    private var cancellable: FlowCancellable?

    init(model: RestockingScreenModel) {
        self.value = model.state.value as? RestockingUiState
        self.cancellable = model.observeState { [weak self] state in
            self?.value = state
        }
    }

    deinit { cancellable?.cancel() }
}
