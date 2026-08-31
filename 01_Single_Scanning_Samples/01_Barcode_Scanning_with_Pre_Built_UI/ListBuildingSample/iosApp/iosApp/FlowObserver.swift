/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Foundation
import shared

// Event-driven bridge from the model's `items` / `scannedCount` Kotlin
// StateFlows to SwiftUI. The shared module exposes callback-based
// `observeItems` / `observeScannedCount` subscriptions backed by coroutine
// collectors on `Dispatchers.Main`, so this side just relays values into
// `@Published` properties — no Timer polling, no spurious re-renders.
final class FlowObserver: ObservableObject {
    @Published var items: [ListBuildingEntry]
    @Published var scannedCount: Int

    private var itemsCancellable: FlowCancellable?
    private var countCancellable: FlowCancellable?

    init(model: ListBuildingScreenModel) {
        self.items = (model.items.value as? [ListBuildingEntry]) ?? []
        self.scannedCount = (model.scannedCount.value as? Int) ?? 0
        self.itemsCancellable = model.observeItems { [weak self] items in
            self?.items = items
        }
        self.countCancellable = model.observeScannedCount { [weak self] count in
            self?.scannedCount = Int(truncating: count)
        }
    }

    deinit {
        itemsCancellable?.cancel()
        countCancellable?.cancel()
    }
}
