/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import shared

// MatrixScan Count results screen — mirrors the native Android
// `ResultsActivity`: a "Scan Results" title bar with a back action, a header
// total, a list split into two sections (items scanned more than once, shown
// as "Non-unique item N" with a quantity badge, then items scanned exactly
// once, shown as "Item N"), a filled Done button whose behaviour depends on
// `doneButtonStyle`, and an outlined Clear List button.
//  - .resume: dismisses, keeping the session.
//  - .newScan: resets the session, then dismisses back to a fresh Scanner.
struct ResultsView: View {
    let screenModel: MatrixScanCountScreenModel
    @ObservedObject var observer: FlowObserver
    let doneButtonStyle: DoneButtonStyle
    let onDone: () -> Void

    private var doneText: String {
        doneButtonStyle == .resume ? "RESUME SCANNING" : "SCAN NEXT ORDER"
    }

    private var nonUniqueItems: [CountedBarcode] {
        observer.items.filter { $0.quantity > 1 }
    }

    private var uniqueItems: [CountedBarcode] {
        observer.items.filter { $0.quantity == 1 }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text("\(observer.totalCount) items")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color.scanditBlack)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)

                Divider()

                List {
                    ForEach(Array(nonUniqueItems.enumerated()), id: \.element.data) { index, item in
                        ResultRow(
                            title: "Non-unique item \(index + 1)",
                            subtitle: "\(item.symbology): \(item.data)",
                            quantity: Int(item.quantity)
                        )
                    }
                    ForEach(Array(uniqueItems.enumerated()), id: \.element.data) { index, item in
                        ResultRow(
                            title: "Item \(index + 1)",
                            subtitle: "\(item.symbology): \(item.data)",
                            quantity: nil
                        )
                    }
                }
                .listStyle(.plain)

                // Filled black primary button (native bg_rectangle_filled).
                Button(action: {
                    if doneButtonStyle == .newScan {
                        screenModel.resetSession()
                    }
                    onDone()
                }) {
                    Text(doneText)
                        .font(.system(size: 16, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(Color.scanditBlack)
                .foregroundColor(.white)
                .padding(.horizontal, 32)
                .padding(.top, 20)

                // Outlined "Clear List" button (native bg_rectangle).
                Button(action: {
                    screenModel.resetSession()
                    onDone()
                }) {
                    Text("CLEAR LIST")
                        .font(.system(size: 16, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .foregroundColor(Color.scanditBlack)
                .overlay(Rectangle().stroke(Color.scanditBlack, lineWidth: 2))
                .padding(.horizontal, 32)
                .padding(.top, 20)
                .padding(.bottom, 28)
            }
            .background(Color.scanditWhite)
            .navigationTitle("Scan Results")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onDone) {
                        Image(systemName: "arrow.left")
                            .foregroundColor(Color.scanditBlack)
                    }
                }
            }
        }
    }
}

private struct ResultRow: View {
    let title: String
    let subtitle: String
    let quantity: Int?

    var body: some View {
        HStack {
            // Placeholder product thumbnail — native shows a 70dp grey square.
            Rectangle()
                .fill(Color(white: 0.67))
                .frame(width: 70, height: 70)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color.scanditBlack)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(Color(red: 0.53, green: 0.58, blue: 0.63))
            }
            .padding(.leading, 24)

            Spacer()

            if let quantity = quantity {
                Text("Qty: \(quantity)")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color.scanditBlack)
            }
        }
        .padding(.vertical, 12)
    }
}
