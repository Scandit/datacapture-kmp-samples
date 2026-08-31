/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI
import UIKit
import shared

// Search phase: a full-screen SparkScanView overlaid on a scanned-barcode
// list, with a nav bar showing how many items have been scanned and a FIND
// button at the bottom. Mirrors SearchScanScreen on Android.

// UIViewRepresentable that embeds the KMP SparkScanView's UIView into SwiftUI.
private struct SparkScanViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }
    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}

// Implements SparkScanViewHandle so the shared model can call onResume/onPause.
private final class SparkScanHandleImpl: SparkScanViewHandle {
    weak var view: SparkScanView?
    func onResume() { view?.onResume() }
    func onPause() { view?.onPause() }
}

// Drives navigation to the Find phase off the SparkScanView's built-in Find
// toolbar button (mirrors SearchScanScreen on Android — no separate button).
private final class SearchUiListenerImpl: SparkScanViewUiListener {
    let onFind: () -> Void
    init(onFind: @escaping () -> Void) { self.onFind = onFind }
    func onBarcodeFindButtonTap(view: SparkScanView) { onFind() }
    func onBarcodeCountButtonTap(view: SparkScanView) {}
    func onLabelCaptureButtonTap(view: SparkScanView) {}
    // Kotlin interface defaults don't bridge as optional to Swift, so every
    // protocol method must be implemented here.
    func onViewStateChanged(newState: SparkScanViewState) {}
}

struct SearchView: View {
    let screenModel: SearchAndFindScreenModel
    let scannedItems: [SearchScannedBarcode]
    let onFind: () -> Void
    let onExit: () -> Void

    // SparkScanView and its handle are created once and held for the view's
    // lifetime via @State so SwiftUI doesn't reconstruct them on re-render.
    @State private var sparkScanView: SparkScanView?
    @State private var handle = SparkScanHandleImpl()
    @State private var uiListener: SearchUiListenerImpl?

    var body: some View {
        NavigationView {
            ZStack {
                // Scanned-barcode list fills the background, mirroring the
                // Android RecyclerView under the SparkScanView overlay.
                VStack(spacing: 0) {
                    List(Array(scannedItems.enumerated()), id: \.offset) { _, item in
                        // Row styled after ListBuildingSample: cropped thumbnail,
                        // bold value over the symbology. No quantity — every scan
                        // is its own row.
                        HStack(spacing: 16) {
                            ScannedThumbnail(image: item.images.first)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.data)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(SwiftUI.Color.scanditBlack)
                                    .lineLimit(2)
                                Text(item.symbology)
                                    .font(.system(size: 12))
                                    .foregroundColor(SwiftUI.Color.scanditBlack.opacity(0.5))
                            }
                            Spacer()
                        }
                        .padding(.vertical, 12)
                    }
                    .listStyle(.plain)
                }

                // SparkScanView floats on top — its trigger button and
                // mini-preview stay reachable while the list is visible.
                if let ssv = sparkScanView {
                    SparkScanViewRepresentable {
                        ssv.toUIView()
                    }
                    .ignoresSafeArea()
                }
            }
            .navigationTitle("\(scannedItems.count) items scanned")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onExit) {
                        Image(systemName: "arrow.left")
                            .foregroundColor(SwiftUI.Color.scanditBlack)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("CLEAR LIST") {
                        screenModel.clearList()
                    }
                    .foregroundColor(SwiftUI.Color.scanditCyan)
                }
            }
        }
        .navigationViewStyle(.stack)
        .onAppear {
            if sparkScanView == nil {
                let view = SparkScanView(
                    dataCaptureContext: screenModel.dataCaptureContext,
                    sparkScan: screenModel.sparkScan,
                    settings: screenModel.sparkScanViewSettings
                )
                // Navigation rides the built-in Find toolbar button + uiListener
                // (matches Android); no separate FIND button.
                view.barcodeFindButtonVisible = true
                let listener = SearchUiListenerImpl(onFind: onFind)
                uiListener = listener
                view.uiListener = listener
                sparkScanView = view
                handle.view = view
                screenModel.registerSparkScanView(handle: handle)
            }
            screenModel.onSearchResumed()
        }
        .onDisappear {
            screenModel.onSearchPaused()
        }
    }
}

// The cropped barcode thumbnail; a grey placeholder when the frame carried no
// image. Mirrors the ListBuildingSample thumbnail (single image, no stack).
private struct ScannedThumbnail: View {
    let image: CroppedImage?

    private let size: CGFloat = 70

    var body: some View {
        ZStack {
            SwiftUI.Color.scanditLightGrey
            if let image = image {
                Image(uiImage: image.toUIImage())
                    .resizable()
                    .scaledToFill()
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 2))
    }
}
