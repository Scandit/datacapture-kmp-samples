/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Foundation
import shared

// Event-driven bridge from the model's `items` / `totalCount` Kotlin
// StateFlows to SwiftUI. The shared module exposes callback-based
// `observeItems` / `observeTotalCount` subscriptions backed by coroutine
// collectors on `Dispatchers.Main`, so this side just relays values into
// `@Published` properties — no Timer polling, no spurious re-renders.
final class FlowObserver: ObservableObject {
    @Published var items: [CountedBarcode]
    @Published var totalCount: Int

    // Read back by ContentView, which holds this object in a `@StateObject` and
    // uses it as the single owner of the model.
    let model: MatrixScanCountScreenModel
    private var itemsCancellable: FlowCancellable?
    private var countCancellable: FlowCancellable?

    init(model: MatrixScanCountScreenModel) {
        self.model = model
        self.items = (model.items.value as? [CountedBarcode]) ?? []
        self.totalCount = (model.totalCount.value as? Int) ?? 0
        self.itemsCancellable = model.observeItems { [weak self] items in
            self?.items = items
        }
        self.countCancellable = model.observeTotalCount { [weak self] count in
            self?.totalCount = Int(truncating: count)
        }
    }

    // ContentView's @StateObject ties this instance's lifetime to the app's
    // single long-lived screenModel, so tearing it down is the iOS
    // equivalent of Android's MainActivity.onDestroy() -> screenModel.dispose().
    deinit {
        itemsCancellable?.cancel()
        countCancellable?.cancel()
        model.dispose()
    }
}
