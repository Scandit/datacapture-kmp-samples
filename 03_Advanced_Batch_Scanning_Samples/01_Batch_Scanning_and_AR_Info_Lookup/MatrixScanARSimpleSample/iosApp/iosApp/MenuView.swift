/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import shared

// Landing screen: Scandit wordmark + title, followed by the mode-picker menu — a 2x2 card grid
// matching the native sample's MenuFragment layout. iOS has no banner vector asset, so the
// wordmark + "MatrixScan AR" header is kept but recolored to the native banner's teal. Each card
// shows an icon, bold title, and subtitle for one ArMode, and navigates into the scanner
// pre-configured for that mode. Camera permission is handled by the system prompt when the
// scanner starts the camera.
struct MenuView: View {
    let onSelectMode: (ArMode) -> Void

    private let modes: [ArMode] = [.highlights, .annotations, .popovers, .statusicons]
    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 24) {
                    ScanditLogo()
                        .fill(Color.scanditBlack)
                        .frame(width: 96, height: 14)
                    Text("MatrixScan AR")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(Color.scanditBannerTeal)
                }
                .frame(maxWidth: 350, alignment: .leading)

                Text("Try These Features")
                    .font(.system(size: 16, weight: .regular))
                    .foregroundColor(Color.scanditBlack)

                LazyVGrid(columns: columns, spacing: 16) {
                    ForEach(modes, id: \.self) { mode in
                        Button(action: { onSelectMode(mode) }) {
                            VStack(alignment: .leading, spacing: 4) {
                                Image(systemName: mode.iconName)
                                    .font(.system(size: 24))
                                    .foregroundColor(.white)
                                Text(mode.title)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                    .lineLimit(1)
                                    .padding(.top, 8)
                                Text(mode.subtitle)
                                    .font(.system(size: 14))
                                    .foregroundColor(.white)
                                    .lineLimit(2)
                                    .padding(.top, 4)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .aspectRatio(1, contentMode: .fit)
                            .padding(12)
                            .background(Color.scanditBlack)
                            .cornerRadius(8)
                        }
                    }
                }
            }
            .padding(.horizontal, 24)
            .frame(maxWidth: 350)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white)
    }
}

extension ArMode {
    var title: String {
        switch self {
        case .highlights: return "Highlights"
        case .annotations: return "Info Annotations"
        case .popovers: return "Popovers"
        case .statusicons: return "Status Icons"
        default: return "Unknown"
        }
    }

    var subtitle: String {
        switch self {
        case .highlights: return "Visualize scanned codes"
        case .annotations: return "Show additional information"
        case .popovers: return "Take action on scanned codes"
        case .statusicons: return "Provide short information"
        default: return ""
        }
    }

    var iconName: String {
        switch self {
        case .highlights: return "viewfinder"
        case .annotations: return "info.circle"
        case .popovers: return "hand.tap"
        case .statusicons: return "exclamationmark.triangle"
        default: return "questionmark"
        }
    }
}
