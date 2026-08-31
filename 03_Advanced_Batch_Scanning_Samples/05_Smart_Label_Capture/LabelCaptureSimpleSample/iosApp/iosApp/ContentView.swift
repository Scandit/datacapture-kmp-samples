/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit

// iOS entry-point view. Mirrors `MainActivity` on Android — just installs
// the navigation bar appearance and routes between `HomeView` and
// `LabelScannerHost`. Per-screen logic lives in the dedicated files.
struct ContentView: View {
    @State private var showScanner: Bool = false

    init() {
        // Light Grey toolbar with Scandit Black content — mirrors the
        // Android Scaffold/TopAppBar choice and stays on-brand without
        // letting a heavy dark bar dominate the camera preview.
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(red: 0xF1 / 255.0, green: 0xF5 / 255.0, blue: 0xF8 / 255.0, alpha: 1)
        let titleColor = UIColor(red: 0x14 / 255.0, green: 0x1F / 255.0, blue: 0x2E / 255.0, alpha: 1)
        appearance.titleTextAttributes = [.foregroundColor: titleColor]
        appearance.largeTitleTextAttributes = [.foregroundColor: titleColor]
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        UINavigationBar.appearance().tintColor = titleColor
    }

    var body: some View {
        NavigationStack {
            HomeView(onStartScan: { showScanner = true })
                .navigationDestination(isPresented: $showScanner) {
                    LabelScannerHost()
                        .navigationTitle("Scan")
                        .navigationBarTitleDisplayMode(.inline)
                }
        }
    }
}
