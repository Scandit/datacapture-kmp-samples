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
final class SearchAndFindHostState: ObservableObject {
    let screenModel: SearchAndFindScreenModel
    @Published var state: SearchAndFindUiState?

    private let observer: FlowObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = SearchAndFindScreenModel()
        self.screenModel = model
        self.observer = FlowObserver(model: model)
        self.state = observer.value
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
    }

    deinit { screenModel.dispose() }
}

// iOS root view. Mirrors `SearchAndFindApp` on Android — routes between
// Home, Search, and Find by switching on the StateFlow value.
struct ContentView: View {
    @StateObject private var host = SearchAndFindHostState()

    init() {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(
            red: 0xF1 / 255.0,
            green: 0xF5 / 255.0,
            blue: 0xF8 / 255.0,
            alpha: 1
        )
        let titleColor = UIColor(
            red: 0x14 / 255.0,
            green: 0x1F / 255.0,
            blue: 0x2E / 255.0,
            alpha: 1
        )
        appearance.titleTextAttributes = [.foregroundColor: titleColor]
        appearance.largeTitleTextAttributes = [.foregroundColor: titleColor]
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        UINavigationBar.appearance().tintColor = titleColor
    }

    var body: some View {
        screenContent
    }

    @ViewBuilder
    private var screenContent: some View {
        let current = host.state
        if current is SearchAndFindUiStateHome || current == nil {
            HomeView(onStartScan: {
                host.screenModel.onEvent(event: SearchAndFindEventNavigateToSearch.shared)
            })
        } else if let search = current as? SearchAndFindUiStateSearch {
            SearchView(
                screenModel: host.screenModel,
                scannedItems: search.scannedItems as? [SearchScannedBarcode] ?? [],
                onFind: {
                    host.screenModel.onEvent(event: SearchAndFindEventNavigateToFind.shared)
                },
                onExit: {
                    host.screenModel.onEvent(event: SearchAndFindEventNavigateBack.shared)
                }
            )
        } else if current is SearchAndFindUiStateFind {
            FindView(
                screenModel: host.screenModel,
                onExit: {
                    host.screenModel.onEvent(event: SearchAndFindEventFinishFind.shared)
                }
            )
        }
    }
}
