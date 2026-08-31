/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit
import shared

// iOS entry-point view. Mirrors `MainActivity` on Android — just installs
// the navigation bar appearance and routes between `MenuView` and
// `MatrixScanARHost`. Per-screen logic lives in the dedicated files.
struct ContentView: View {
    @State private var selectedMode: ArMode?

    init() {
        // Black toolbar with white content — mirrors the native Android sample's
        // fragment_scan.xml toolbar (black background, centered white 18sp title, no back
        // arrow). Applied globally via UINavigationBar.appearance() since MenuView never shows
        // its nav bar (no navigationTitle set on the root screen).
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = .black
        let titleColor = UIColor.white
        appearance.titleTextAttributes = [.foregroundColor: titleColor]
        appearance.largeTitleTextAttributes = [.foregroundColor: titleColor]
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        UINavigationBar.appearance().tintColor = titleColor
    }

    var body: some View {
        NavigationStack {
            MenuView(onSelectMode: { mode in selectedMode = mode })
                // `navigationDestination(item:)` is iOS 17+, but this sample targets iOS 16 —
                // drive the push off a Bool binding derived from the optional selected mode.
                .navigationDestination(
                    isPresented: Binding(
                        get: { selectedMode != nil },
                        set: { if !$0 { selectedMode = nil } }
                    )
                ) {
                    if let mode = selectedMode {
                        MatrixScanARHost(mode: mode, onExit: { selectedMode = nil })
                            .navigationTitle(mode.title)
                            .navigationBarTitleDisplayMode(.inline)
                            .navigationBarBackButtonHidden(true)
                    }
                }
        }
    }
}

extension ArMode: @retroactive Identifiable {
    // Kotlin `enum class` bridges `name` (and `ordinal`) to Swift but has no `rawValue`.
    public var id: String { self.name }
}
