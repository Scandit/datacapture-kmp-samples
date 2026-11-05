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

// Which screen the app is showing. Keeping this in one value means the camera is
// always switched from a known screen rather than inferred: `.scanner` is the
// only state in which scanning should be running.
private enum Screen: Equatable {
    case home
    case scanner
    case results(DoneButtonStyle)
}

// App entry point. It holds one long-lived `MatrixScanCountScreenModel` — and so
// one `DataCaptureContext` and one `BarcodeCount` mode — for the whole app, which
// is why the counted items survive moving between the scanner and the results
// list.
//
// Note how the results list is layered over the scanner in a `ZStack` rather than
// presented with `.fullScreenCover` or `.sheet`. That is worth copying:
// `BarcodeCountView` keeps scanning while it is on screen, and a modal
// presentation covers it without giving your code any callback to restart it from
// when the modal goes away, so the preview stays frozen after the user comes
// back. Layering keeps the view on screen and untouched, so returning to the
// scanner needs nothing beyond switching the camera back on.
struct ContentView: View {
    // The observer owns the model, and `@StateObject` builds it exactly once for
    // the lifetime of the app. Build it here rather than in an `init()`: SwiftUI
    // re-initialises view structs freely, and each `init()` would construct
    // another DataCaptureContext, BarcodeCount and Camera while the retained
    // observer kept watching the first one.
    @StateObject private var observer = FlowObserver(model: MatrixScanCountScreenModel())

    private var screenModel: MatrixScanCountScreenModel { observer.model }

    @State private var screen: Screen = .home
    @Environment(\.scenePhase) private var scenePhase

    // `navigationDestination` wants a Bool; derive it so Home can never
    // disagree with `screen`. Popping the stack lands back on `.home`.
    private var isScanning: Binding<Bool> {
        Binding(
            get: { screen != .home },
            set: { presented in
                if !presented {
                    screen = .home
                }
            }
        )
    }

    private var resultsStyle: DoneButtonStyle? {
        if case .results(let style) = screen {
            return style
        }
        return nil
    }

    var body: some View {
        ZStack {
            NavigationStack {
                HomeView(onStartScan: { screen = .scanner })
                    .navigationDestination(isPresented: isScanning) {
                        ScannerView(
                            screenModel: screenModel,
                            onListTap: { screen = .results(.resume) },
                            onExitTap: { screen = .results(.newScan) }
                        )
                    }
            }
            // The results list is an opaque sibling here, not a modal, so the
            // stack below it stays live: hide it from VoiceOver and take it out
            // of hit testing, or the scanner's buttons are still reachable
            // behind the list.
            .accessibilityHidden(resultsStyle != nil)
            .allowsHitTesting(resultsStyle == nil)

            if let style = resultsStyle {
                ResultsView(
                    screenModel: screenModel,
                    observer: observer,
                    doneButtonStyle: style,
                    onDone: { screen = .scanner }
                )
            }
        }
        // One place decides what the camera and the mode do on each screen. Doing
        // this per screen — rather than from each view's appearance callbacks —
        // keeps a single owner of the capture state:
        //  - .scanner: enable the mode and switch the camera on. Needed on first
        //    entry and on every return, because the pre-built Finish button
        //    switches both off when it is tapped.
        //  - .results reached from the list button: leave the camera running so
        //    returning is instant, but stop publishing scans, or clearing the
        //    list would refill it immediately from the still-running scanner.
        //    Reached from the exit button the camera is already off, because the
        //    pre-built exit button switches it off itself — and so it is after
        //    backgrounding the app on either path, since only `.scanner` resumes
        //    it. Returning to the scanner switches it back on either way.
        //  - .home: nothing is scanning, so switch the camera off. The context and
        //    the mode stay alive; only `dispose()` releases them.
        .onChange(of: screen) { newScreen in
            let onScanner = newScreen == .scanner
            if onScanner {
                screenModel.resumeFrameSource()
            } else if newScreen == .home {
                screenModel.pauseFrameSource()
            }
            screenModel.setUpdatesEnabled(enabled: onScanner)
        }
        // Backgrounding the app changes no screen and fires no appearance
        // callback, so the camera needs its own edge here or the preview is black
        // after a minimize and reopen. Same reason the Android host observes the
        // activity lifecycle instead of the composition.
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                if screen == .scanner {
                    screenModel.resumeFrameSource()
                }
            default:
                screenModel.pauseFrameSource()
            }
        }
    }
}
