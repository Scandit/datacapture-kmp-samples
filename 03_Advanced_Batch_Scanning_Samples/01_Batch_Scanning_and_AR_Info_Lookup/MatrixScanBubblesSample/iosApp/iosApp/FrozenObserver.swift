/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Foundation
import shared

// Event-driven bridge from `MatrixScanBubblesScreenModel.frozen` (Kotlin
// StateFlow<Boolean>) to SwiftUI. The shared module exposes `observeFrozen` —
// a callback-based subscription backed by a coroutine collector on
// `Dispatchers.Main` — so this side just relays values into a `@Published`
// property. No Timer polling, no spurious re-renders: the publisher fires only
// when the upstream StateFlow emits.
final class FrozenObserver: ObservableObject {
    @Published var value: Bool = false

    private var cancellable: FlowCancellable?

    init(model: MatrixScanBubblesScreenModel) {
        self.value = model.isFrozen()
        self.cancellable = model.observeFrozen { [weak self] frozen in
            self?.value = frozen.boolValue
        }
    }

    deinit { cancellable?.cancel() }
}
