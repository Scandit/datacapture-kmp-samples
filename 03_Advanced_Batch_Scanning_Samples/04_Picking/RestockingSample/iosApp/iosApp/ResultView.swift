/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import shared

// Static breakdown shown after the BarcodePickView's built-in finish button is
// tapped: a Picklist section (everything picked) followed by an Inventory
// section (scanned but not picked). Mirrors ResultScreen on Android and the
// native sample's ProductListFragment — "Result List" top bar, grey
// product-image placeholder, teal/amber status icons, and the Continue
// scanning / Finish button pair.
struct ResultView: View {
    let items: [ListItem]
    let onContinueScanning: () -> Void
    let onFinishPickup: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // The back chevron returns to the picking session, same as
            // "Continue scanning" (matches the native up arrow).
            HStack(spacing: 8) {
                Button(action: onContinueScanning) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(SwiftUI.Color.white)
                }
                Text("Result List")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(SwiftUI.Color.white)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(SwiftUI.Color.scanditBlack)

            List {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    if let header = item as? RestockingHeader {
                        SectionHeaderRow(header: header)
                            .listRowInsets(EdgeInsets())
                    } else if let row = item as? RestockingProductRow {
                        ProductRowView(product: row)
                            .listRowInsets(EdgeInsets())
                    }
                }
            }
            .listStyle(.plain)

            VStack(spacing: 16) {
                Button(action: onContinueScanning) {
                    Text("Continue scanning")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(SwiftUI.Color.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(SwiftUI.Color.scanditBlack)
                .cornerRadius(8)

                Button(action: onFinishPickup) {
                    Text("Finish")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(SwiftUI.Color.scanditBlack)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .background(SwiftUI.Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(SwiftUI.Color.scanditBlack900, lineWidth: 1)
                )
            }
            .padding(.horizontal, 32)
            .padding(.vertical, 16)
        }
    }
}

private struct SectionHeaderRow: View {
    let header: RestockingHeader

    var body: some View {
        Text(header.isPickList ? "Picklist" : "Inventory (\(header.count))")
            .font(.system(size: 13, weight: .bold))
            .foregroundColor(SwiftUI.Color.scanditBlack900)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
    }
}

private struct ProductRowView: View {
    let product: RestockingProductRow

    private var isUnknown: Bool { product.quantityToPick == 0 && product.picked }

    var body: some View {
        HStack(spacing: 16) {
            // Grey product-image placeholder (native item_result.xml imageView).
            Rectangle()
                .fill(SwiftUI.Color.scanditGrey500)
                .frame(width: 56, height: 56)

            VStack(alignment: .leading, spacing: 2) {
                Text(product.identifier)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(SwiftUI.Color.scanditBlack900)
                Text("GTIN: \(product.barcodeData)")
                    .font(.system(size: 14))
                    .foregroundColor(SwiftUI.Color.scanditGrey500)
                if isUnknown {
                    Text("Picked item not in pick list")
                        .font(.system(size: 12))
                        .foregroundColor(SwiftUI.Color.scanditRed)
                }
            }

            Spacer()

            statusIcon
        }
        .padding(16)
    }

    @ViewBuilder
    private var statusIcon: some View {
        if isUnknown {
            Image(systemName: "exclamationmark.circle.fill")
                .font(.system(size: 26))
                .foregroundColor(SwiftUI.Color.scanditAmber)
        } else if product.picked {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 26))
                .foregroundColor(SwiftUI.Color.scanditTeal)
        }
    }
}
