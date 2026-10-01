/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import shared

// Which "Done" semantics the Results screen should use — mirrors the native
// Android sample's `ResultsActivity.DoneButtonStyle`.
enum DoneButtonStyle {
    /// Reached via the list button. Done just dismisses; the session is untouched.
    case resume
    /// Reached via the exit button. Done resets the session, then returns to a fresh Scanner.
    case newScan
}

private enum Route: Identifiable, Equatable {
    case results(DoneButtonStyle)

    var id: String {
        switch self {
        case .results(.resume): return "resume"
        case .results(.newScan): return "newScan"
        }
    }
}

// iOS entry-point view. Owns the single long-lived MatrixScanCountScreenModel
// for the app's lifetime and routes between the Home landing and the Scanner
// via a NavigationStack, so the device back button returns to Home. Results is
// a full-screen-cover over the Scanner — a Bool/enum-driven presentation
// rather than `navigationDestination(item:)`, which needs iOS 17 (this sample
// targets iOS 16).
struct ContentView: View {
    @StateObject private var observer: FlowObserver
    private let screenModel: MatrixScanCountScreenModel

    @State private var showScanner = false
    @State private var route: Route?
    @State private var barcodeCountView: BarcodeCountView?

    init() {
        let model = MatrixScanCountScreenModel()
        self.screenModel = model
        self._observer = StateObject(wrappedValue: FlowObserver(model: model))
    }

    var body: some View {
        NavigationStack {
            HomeView(onStartScan: { showScanner = true })
                .navigationDestination(isPresented: $showScanner) {
                    ScannerView(
                        screenModel: screenModel,
                        onListTap: { route = .results(.resume) },
                        onExitTap: { route = .results(.newScan) },
                        onViewCreated: { barcodeCountView = $0 }
                    )
                    .fullScreenCover(item: $route) { activeRoute in
                        switch activeRoute {
                        case .results(let style):
                            ResultsView(
                                screenModel: screenModel,
                                observer: observer,
                                doneButtonStyle: style,
                                onDone: { route = nil }
                            )
                        }
                    }
                    // Keep the camera and mode running while Results is shown
                    // (fullScreenCover leaves ScannerView mounted underneath) so
                    // returning resumes instantly. Freeze the results list while
                    // Results is visible so a Clear List isn't refilled by the
                    // still-running scanner.
                    .onChange(of: route) { newRoute in
                        screenModel.setUpdatesEnabled(enabled: newRoute == nil)
                    }
                }
        }
    }
}
